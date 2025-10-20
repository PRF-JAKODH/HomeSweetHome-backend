package com.homesweet.homesweetback.domain.auth.entity;

import com.homesweet.homesweetback.common.BaseEntity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@AllArgsConstructor
@Builder
@Entity
@Getter
@NoArgsConstructor
@Setter
@Table(name = "users", uniqueConstraints = {
	@UniqueConstraint(name = "uk_user_email", columnNames = {"email"}),
	@UniqueConstraint(name = "uk_user_provider_id", columnNames = {"provider", "provider_id"})
})
public class User extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 255)
	private String email;

	@Column(nullable = false, length = 50)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OAuth2Provider provider;

	@Column(name = "provider_id", length = 255)
	private String providerId; // OAuth Provider의 사용자 ID

	@Column(name = "profile_image_url", length = 500)
	private String profileImageUrl;

    @Column(name = "grade", nullable = true)
    private String grade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "birth_date")
    private LocalDate birthDate;

	/**
	 * OAuth 사용자인지 확인합니다.
	 * 이 서비스는 모든 사용자가 OAuth 사용자입니다.
	 * 
	 * @return 항상 true (모든 사용자가 OAuth 사용자)
	 */
	public boolean isOAuthUser() {
		return true;
	}

	/**
	 * 특정 Provider의 사용자인지 확인합니다.
	 * 
	 * @param provider 확인할 Provider
	 * @return 해당 Provider의 사용자이면 true
	 */
	public boolean isSameProvider(OAuth2Provider provider) {
		return this.provider == provider;
	}
}
