package com.nigealm.usermanagement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Document(collection = "users")
public class UserEntity implements UserDetails
{
	private static final long serialVersionUID = 6740577818002093997L;

	public static final String ADMINISTRATOR_USER_NAME = "snapglueg@gmail.com";
	public static final String ADMINISTRATOR_USER_ROLE = UserRole.ROLE_ADMIN.getRole();
	public static final String ADMINISTRATOR_USER_EMAIL = "snapglueg@gmail.com";
	public static final String ADMINISTRATOR_USER_PASSWORD = "snapgluethebest";
	public static final String ADMINISTRATOR_USER_PICTURE_LINK = null;
	public static final boolean ADMINISTRATOR_USER_ENABLED = true;
	public static final boolean ADMINISTRATOR_USER_ACCOUNT_NON_EXPIRED = true;
	public static final boolean ADMINISTRATOR_USER_CREDENTIALS_NON_EXPIRED = true;
	public static final boolean ADMINISTRATOR_USER_ACCOUNT_NON_LOCKED = true;
	public static final String ADMINISTRATOR_USER_TENENT = "none";
	public static final String ADMINISTRATOR_USER_DATA = null;
	
	@Id
	@GeneratedValue
	private Long id;
	private String username;
	private String role;
	private String email;
	private String password;
	private String pictureLink;
	private boolean enabled;
	private boolean accountNonExpired;
	private boolean credentialsNonExpired;
	private boolean accountNonLocked;
	private String tenant;

	@Lob
	private String data;

	protected UserEntity()
	{
		// protected - to avoid creation of empty entity
	}

	public UserEntity(String username, String role, String email, String password, String pictureLink, boolean enabled,
			boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked, String tenant,
			String data)	{
		this.username = username;
		this.role = role;
		this.email = email;
		this.password = password;
		this.pictureLink = pictureLink;
		this.enabled = enabled;
		this.accountNonExpired = accountNonExpired;
		this.credentialsNonExpired = credentialsNonExpired;
		this.accountNonLocked = accountNonLocked;
		this.tenant = tenant;
		this.data = data;
	}

	public String getRole()
	{
		return this.role;
	}

	public void setRole(String role) 
	{
		this.role = role;
	}

	public String getData()
	{
		return data;
	}

	public void setData(String data)
	{
		this.data = data;
	}

	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username;
	}

	public String getEmail()
	{
		return email;
	}

	public void setEmail(String email)
	{
		this.email = email;
	}

	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public String getPictureLink()
	{
		return pictureLink;
	}

	public void setPictureLink(String pictureLink)
	{
		this.pictureLink = pictureLink;
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	public boolean isAccountNonExpired()
	{
		return accountNonExpired;
	}

	public void setAccountNonExpired(boolean accountNonExpired)
	{
		this.accountNonExpired = accountNonExpired;
	}

	public boolean isCredentialsNonExpired()
	{
		return credentialsNonExpired;
	}

	public void setCredentialsNonExpired(boolean credentialsNonExpired)
	{
		this.credentialsNonExpired = credentialsNonExpired;
	}

	public boolean isAccountNonLocked()
	{
		return accountNonLocked;
	}

	public void setAccountNonLocked(boolean accountNonLocked)
	{
		this.accountNonLocked = accountNonLocked;
	}

	public String getTenant()
	{
		return tenant;
	}

	public void setTenant(String tenant)
	{
		this.tenant = tenant;
	}

	@Override
	public String toString()
	{
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities()
	{
		SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority(getRole());
		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		authorities.add(simpleGrantedAuthority);
		return authorities;
	}

}
