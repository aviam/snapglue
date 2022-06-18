package com.nigealm.usermanagement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nigealm.common.utils.Tracer;

@Service("userDetailsService")
public class UserDetailsServiceImpl implements UserDetailsService
{
	private static final Tracer tracer = new Tracer(UserDetailsServiceImpl.class);

	@Autowired
	private UserManagementDao userManagementDao;
	@Autowired
	private Assembler assembler;
    private String username;
	public UserDetailsServiceImpl()
	{
	}

	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException, DataAccessException
	{  this.username=userName;
		tracer.entry("loadUserByUsername");
		UserEntity userEntity = userManagementDao.getUserByName(userName);
		tracer.trace("UserEntity:" + userEntity);
		if (userEntity == null)
		{
			tracer.trace("user name: " + userName + " not found in the database");
			throw new UsernameNotFoundException("name not found");
		}
		tracer.exit("loadUserByUsername");
		return assembler.buildUserFromUserEntity(userEntity);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((username == null) ? 0 : username.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserDetailsServiceImpl other = (UserDetailsServiceImpl) obj;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}
	
	

	
	
	
	 
}