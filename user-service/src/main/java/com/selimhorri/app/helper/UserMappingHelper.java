package com.selimhorri.app.helper;

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;

public interface UserMappingHelper {
	
	public static UserDto map(final User user) {
	final Credential credential = user.getCredential();
	final CredentialDto credentialDto = (credential == null)
		? null
		: CredentialDto.builder()
		    .credentialId(credential.getCredentialId())
		    .username(credential.getUsername())
		    .password(credential.getPassword())
		    .roleBasedAuthority(credential.getRoleBasedAuthority())
		    .isEnabled(credential.getIsEnabled())
		    .isAccountNonExpired(credential.getIsAccountNonExpired())
		    .isAccountNonLocked(credential.getIsAccountNonLocked())
		    .isCredentialsNonExpired(credential.getIsCredentialsNonExpired())
		    .build();

	return UserDto.builder()
		.userId(user.getUserId())
		.firstName(user.getFirstName())
		.lastName(user.getLastName())
		.imageUrl(user.getImageUrl())
		.email(user.getEmail())
		.phone(user.getPhone())
		.credentialDto(credentialDto)
		.build();
	}
	
	public static User map(final UserDto userDto) {
	final CredentialDto credentialDto = userDto.getCredentialDto();
	final Credential credential = (credentialDto == null)
		? null
		: Credential.builder()
		    .credentialId(credentialDto.getCredentialId())
		    .username(credentialDto.getUsername())
		    .password(credentialDto.getPassword())
		    .roleBasedAuthority(credentialDto.getRoleBasedAuthority())
		    .isEnabled(credentialDto.getIsEnabled())
		    .isAccountNonExpired(credentialDto.getIsAccountNonExpired())
		    .isAccountNonLocked(credentialDto.getIsAccountNonLocked())
		    .isCredentialsNonExpired(credentialDto.getIsCredentialsNonExpired())
		    .build();

	return User.builder()
		.userId(userDto.getUserId())
		.firstName(userDto.getFirstName())
		.lastName(userDto.getLastName())
		.imageUrl(userDto.getImageUrl())
		.email(userDto.getEmail())
		.phone(userDto.getPhone())
		.credential(credential)
		.build();
	}
	
	
	
}






