package dev.zap.JobPortalLogin.appuser;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor 
public class AppUserService implements UserDetailsService {
    
    private final static String USER_NOT_FOUND_MSG = "user with email %s not found";
    
    private final AppUserRepository appUserRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    // private final ConfirmationTokenService confirmationTokenService; 
 
    // Manual constructor for dependency injection without AllArgsConstructor
 	 public AppUserService(AppUserRepository appUserRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
 	        this.appUserRepository = appUserRepository;
 	        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
 	    }
    @Override
    public UserDetails loadUserByUsername(String email) 
            throws UsernameNotFoundException {
        
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> 
                    new UsernameNotFoundException(String.format(USER_NOT_FOUND_MSG, email)));
    }
    
    public String signUpUser(AppUser appUser) {
        boolean userExists = appUserRepository
                .findByEmail(appUser.getUsername()) 
                .isPresent();

        if (userExists) {
            throw new IllegalStateException("Email already taken");
        }

        String encodedPassword = bCryptPasswordEncoder.encode(appUser.getPassword());
        appUser.setPassword(encodedPassword); 
        appUserRepository.save(appUser);
        
        // TODO: Send 200 token
        
        return "YESS! IT WORKS";
        
    }
}
