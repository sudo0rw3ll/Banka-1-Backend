package rs.edu.raf.banka1.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import rs.edu.raf.banka1.mapper.UserMapper;
import rs.edu.raf.banka1.model.Permission;
import rs.edu.raf.banka1.model.User;
import rs.edu.raf.banka1.repositories.PermissionRepository;
import rs.edu.raf.banka1.model.User;
import rs.edu.raf.banka1.repositories.UserRepository;
import rs.edu.raf.banka1.responses.ActivateAccountResponse;
import rs.edu.raf.banka1.responses.CreateUserResponse;
import rs.edu.raf.banka1.responses.EditUserResponse;
import rs.edu.raf.banka1.responses.UserResponse;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private UserMapper userMapper;
    private UserRepository userRepository;
    private PermissionRepository permissionRepository;
    private EmailService emailService;
    @Autowired
    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, EmailService emailService, PermissionRepository permissionRepository) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.emailService = emailService;
        this.permissionRepository = permissionRepository;
    }

    public UserResponse findByEmail(String email) {
        return this.userRepository.findByEmail(email).map(userMapper::userToUserResponse).orElse(null);
    }

    @Override
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(userMapper::userToUserResponse).toList();
    }

    @Override
    public UserResponse findById(Long id) {
        return userRepository.findById(id).map(userMapper::userToUserResponse).orElse(null);
    }

    @Override
    public List<UserResponse> search(String email, String firstName, String lastName, String position) {
        return userRepository.searchUsersByEmailAndFirstNameAndLastNameAndPosition(email, firstName, lastName, position)
                .map(users -> users.stream().map(userMapper::userToUserResponse).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    @Override
    public CreateUserResponse createUser(String email, String password, String firstName, String lastName, String jmbg, String position, String phoneNumber, boolean isActive) {
        User user = populateUser(email, password, firstName, lastName, jmbg, position, phoneNumber, isActive);
        userRepository.save(user);
        return new CreateUserResponse(user.getUserId());
    }


    @Override
    public CreateUserResponse createUser(String email, String password, String firstName, String lastName, String jmbg, String position, String phoneNumber, boolean isActive, String activationToken) {
        User user = populateUser(email, password, firstName, lastName, jmbg, position, phoneNumber, isActive);
        user.setActivationToken(activationToken);
        userRepository.save(user);
        emailService.sendActivationEmail(email, "RAF Banka - User activation", "Visit this URL to activate your account: http://localhost:8080/user/activate/" + activationToken);
        return new CreateUserResponse(user.getUserId());
    }

    private User populateUser(String email, String password, String firstName, String lastName, String jmbg, String position, String phoneNumber, boolean isActive) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setJmbg(jmbg);
        user.setPosition(position);
        user.setPhoneNumber(phoneNumber);
        user.setActive(isActive);
        user.setPassword(password);
        return user;
    }

    @Override
    public ActivateAccountResponse activateAccount(String token, String password) {
        User user = userRepository.findByActivationToken(token).get();
        user.setActivationToken(null);
        user.setPassword(password);
        userRepository.save(user);
        return new ActivateAccountResponse(user.getUserId());
    }

    @Override
    public EditUserResponse editUser(String email, String password, String firstName, String lastName, String jmbg, String position, String phoneNumber, boolean isActive, Set<String> permissions) {
        User user = userRepository.findByEmail(email).get();
        user.setPassword(password);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setJmbg(jmbg);
        user.setPosition(position);
        user.setPhoneNumber(phoneNumber);
        user.setActive(isActive);
        user.setPermissions(permissions.stream().map(perm -> permissionRepository.findByName(perm).get()).collect(Collectors.toSet()));
        userRepository.save(user);
        return new EditUserResponse(user.getUserId());
    }

    //necessary for authentication
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> myUser = this.userRepository.findByEmail(username);
        if(myUser.isEmpty()) {
            throw new UsernameNotFoundException("User name " + username + " not found");
        }

        User user = myUser.get();
        //convert permissions to list of simple granted authorities used by @PreAuthorize
        List<SimpleGrantedAuthority> authorities = user.getPermissions().stream()
                .map((permission -> new SimpleGrantedAuthority(permission.getName())))
                .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), authorities);
    }
}
