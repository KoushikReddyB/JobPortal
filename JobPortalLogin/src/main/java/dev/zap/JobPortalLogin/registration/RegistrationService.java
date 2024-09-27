package dev.zap.JobPortalLogin.registration;

import org.springframework.stereotype.Service;

import dev.zap.JobPortalLogin.appuser.AppUser;
import dev.zap.JobPortalLogin.appuser.AppUserRole;
import dev.zap.JobPortalLogin.appuser.AppUserService;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class RegistrationService {
	
	private final AppUserService appUserService;
	private final EmailValidator emailValidator;
	
    // Manual constructor for dependency injection without AllArgsConstructor
	 public RegistrationService(AppUserService appUserService, EmailValidator emailValidator) {
	        this.appUserService = appUserService;
	        this.emailValidator = emailValidator;
	    }
    
	public String register(RegistrationRequest request) {
		boolean isValidEmail = emailValidator.test(request.getEmail());
		
		if(!isValidEmail) {
			throw new IllegalStateException("Email is not Valid!!");
		} 
		
		return appUserService.signUpUser(
				new AppUser(
						request.getFirstName(),
						request.getLastName(),
						request.getEmail(),
						request.getPassword(),
						AppUserRole.USER
						)
				);
	}
	public String confirmToken(String token) {
		return token;
	}
}
