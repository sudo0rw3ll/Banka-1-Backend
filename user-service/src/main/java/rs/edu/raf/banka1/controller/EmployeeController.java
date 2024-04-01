package rs.edu.raf.banka1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import rs.edu.raf.banka1.dtos.PermissionDto;
import rs.edu.raf.banka1.dtos.employee.CreateEmployeeDto;
import rs.edu.raf.banka1.dtos.employee.EditEmployeeDto;
import rs.edu.raf.banka1.dtos.employee.EmployeeDto;
import rs.edu.raf.banka1.requests.*;
import rs.edu.raf.banka1.responses.*;
import rs.edu.raf.banka1.services.EmployeeService;

import java.util.List;

@RestController
@RequestMapping("/employee")
@Tag(name = "Employee", description = "Employee Controller")
@SecurityRequirement(name = "basicScheme")
@CrossOrigin
public class EmployeeController {

    private final EmployeeService employeeService;
    private final PasswordEncoder passwordEncoder;

    public EmployeeController(EmployeeService employeeService,
                              PasswordEncoder passwordEncoder){
        this.employeeService = employeeService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping(value = "/getAll", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all users", description = "Returns all users")
    @PreAuthorize("hasAuthority('readUser')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = List.class,
                                    subTypes = {UserResponse.class}))}),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<EmployeeDto>> readAllUsers() {
        return new ResponseEntity<>(this.employeeService.findAll(), HttpStatus.OK);
    }

    @GetMapping(value = "/get/{email}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get user by email", description = "Returns user by email")
    @PreAuthorize("hasAuthority('readUser')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmployeeDto.class))}),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<EmployeeDto> readUser(@PathVariable String email) {
        return new ResponseEntity<>(this.employeeService.findByEmail(email), HttpStatus.OK);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get user by id", description = "Returns user by id")
    @PreAuthorize("hasAuthority('readUser')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class))}),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<EmployeeDto> readUser(@PathVariable Long id) {
        return new ResponseEntity<>(this.employeeService.findById(id), HttpStatus.OK);
    }

    @GetMapping(value = "/getUser", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get user by jwt", description = "Returns user by jwt")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmployeeDto.class))}),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<EmployeeDto> readUserFromJwt() {
        EmployeeDto employeeDto = this.employeeService.findByJwt();
        if (employeeDto == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<EmployeeDto>(employeeDto, HttpStatus.OK);
    }

    @GetMapping(value = "/search")
    @Operation(summary = "Search and filter users", description = "Returns users by e-mail, last name and/or position.")
    @PreAuthorize("hasAuthority('readUser')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = List.class,
                                    subTypes = {EmployeeDto.class}))}),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<EmployeeDto>> searchUsers(
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "firstName", required = false) String firstName,
            @RequestParam(name = "lastName", required = false) String lastName,
            @RequestParam(name = "position", required = false) String position
    ) {
        return new ResponseEntity<>(this.employeeService.search(email, firstName, lastName, position), HttpStatus.OK);
    }

    @PostMapping(value = "/createUser")
    @Operation(summary = "Admin create user", description = "Creates a new user with the specified params, and forwards an activation mail to the user.")
    @PreAuthorize("hasAuthority('addUser')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User created successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Long.class))}),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<CreateUserResponse> createUser(@RequestBody CreateEmployeeDto createEmployeeDto) {
        return new ResponseEntity<CreateUserResponse>(employeeService.createUser(createEmployeeDto), HttpStatus.OK);
    }

    @PostMapping(value = "/activate/{token}")
    @Operation(summary = "Activate account", description = "Activate an account by assigning a password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account activated successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = ActivateAccountResponse.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid token or password"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ActivateAccountResponse> activateAccount(@PathVariable String token, @RequestBody ActivateAccountRequest activateAccountRequest) {
        String password = activateAccountRequest.getPassword();
        if (password.length() < 4) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(employeeService.activateAccount(token, password), HttpStatus.OK);
    }

    @PostMapping(value = "/reset/{email}")
    @Operation(summary = "Reset password", description = "Send password reset email to user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email with password reset URL sent successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = ActivateAccountResponse.class))}),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> sendResetPasswordEmail(@PathVariable String email) {
        employeeService.sendResetPasswordEmail(email);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping(value = "/newpassword/{token}")
    @Operation(summary = "Activate account", description = "Activate an account by assigning a password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account activated successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = ActivateAccountResponse.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid token or password"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<NewPasswordResponse> setNewPassword(@PathVariable String token, @RequestBody NewPasswordRequest newPasswordRequest) {
        String password = newPasswordRequest.getPassword();
        if (password.length() < 4) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        NewPasswordResponse newPasswordResponse = employeeService.setNewPassword(token, password);
        return new ResponseEntity<>(newPasswordResponse, newPasswordResponse.getUserId() != null ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
    }

    @PutMapping("/")
    @Operation(summary = "Admin edit user", description = "Admin can edit a user's info")
    @PreAuthorize("hasAuthority('modifyUser')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User edited successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = EditEmployeeDto.class))}),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Boolean> editUser(@RequestBody EditEmployeeDto editUserRequest) {
        boolean edited = employeeService.editUser(editUserRequest);
        return new ResponseEntity<>(edited, edited ? HttpStatus.OK : HttpStatus.NOT_FOUND);
    }

    @PostMapping(value = "permission/{userId}")
    @Operation(summary = "Change permissions to user", description = "Change permissions to user")
    @PreAuthorize("hasAuthority('modifyUser')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permissions changed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class))}),
            @ApiResponse(responseCode = "404", description = "Bad request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Boolean> changePermissions(@PathVariable Long userId,
                                                     @RequestBody ModifyPermissionsRequest request) {
        boolean changed = employeeService.modifyUserPermissions(request, userId);
        return new ResponseEntity<>(changed, changed ? HttpStatus.OK : HttpStatus.NOT_FOUND);
    }

    @DeleteMapping(value = "/remove/{id}")
    @Operation(summary = "Admin delete user", description = "Admin can delete a user")
    @PreAuthorize("hasAuthority('deleteUser')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User deleted successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class))}),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Boolean> deleteUser(@PathVariable Long id) {
        Boolean deleted = employeeService.deleteUser(id);
        return new ResponseEntity<>(deleted, deleted ? HttpStatus.OK : HttpStatus.NOT_FOUND);
    }

    @GetMapping(value = "/permissions/userId/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all permissions of user", description = "Returns all permissions")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = List.class,
                                    subTypes = {PermissionDto.class}))}),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<PermissionDto>> readAllPermissions(@PathVariable Long userId) {
        return new ResponseEntity<>(this.employeeService.findPermissions(userId), HttpStatus.OK);
    }


    @GetMapping(value = "/permissions/email/{email}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all permissions of user", description = "Returns all permissions")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = List.class,
                                    subTypes = {PermissionDto.class}))}),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<PermissionDto>> readAllPermissions(@PathVariable String email) {
        return new ResponseEntity<>(this.employeeService.findPermissions(email), HttpStatus.OK);
    }


}