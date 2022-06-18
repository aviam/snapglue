package com.nigealm.usermanagement;

import java.util.ArrayList;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nigealm.common.utils.Tracer;

@Service("assembler")
public class Assembler
{
	private static final Tracer tracer = new Tracer(Assembler.class);

	@Transactional(readOnly = true)
	User buildUserFromUserEntity(UserEntity userEntity)
	{
		tracer.entry("buildUserFromUserEntity");
		String username = userEntity.getUsername();
		String password = userEntity.getPassword();
		boolean enabled = userEntity.isEnabled();
		boolean accountNonExpired = userEntity.isAccountNonExpired();
		boolean credentialsNonExpired = userEntity.isCredentialsNonExpired();
		boolean accountNonLocked = userEntity.isAccountNonLocked();

		Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		// if other roles are needed - take from roles table..
		// for (SecurityRoleEntity role : userEntity.getRoles())
		// {
		// authorities.add(new GrantedAuthorityImpl(role.getRoleName()));
		// }
//		authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
		authorities.add(new SimpleGrantedAuthority(userEntity.getRole()));
		User user = new User(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked,
				authorities);

		tracer.trace("User: " + user.toString());

		tracer.exit("buildUserFromUserEntity");
		return user;
	}
}