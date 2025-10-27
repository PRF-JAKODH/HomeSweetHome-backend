package com.homesweet.homesweetback.domain.auth.entity;

import com.homesweet.homesweetback.common.BaseEntity;
import com.homesweet.homesweetback.domain.grade.entity.Grade;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@AllArgsConstructor
@Builder
@Entity
@Getter
@NoArgsConstructor
@Setter
@Table(name = "users")
public class User extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long id;

	@Column(nullable = false, length = 100)
	private String email;

	@Column(nullable = false, length = 20)
	private String name;

	@Column(nullable = true, length = 100)
	private String address;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OAuth2Provider provider;

	@Column(name = "provider_id", length = 255)
	private String providerId; // OAuth Provider의 사용자 ID

	@Column(name = "profile_img_url", length = 255)
	private String profileImageUrl;

    @ManyToOne
    @JoinColumn(name = "grade_id", nullable = true)
    private Grade grade;

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

	/**
	 * 사용자의 등급을 Optional로 반환합니다.
	 * 등급이 없는 경우 빈 Optional을 반환합니다.
	 * 
	 * @return 사용자의 등급이 있으면 Optional.of(grade), 없으면 Optional.empty()
	 */
	public Optional<Grade> getGradeOptional() {
		return Optional.ofNullable(this.grade);
	}

	/**
	 * 사용자가 등급을 가지고 있는지 확인합니다.
	 * 
	 * @return 등급이 있으면 true, 없으면 false
	 */
	public boolean hasGrade() {
		return this.grade != null;
	}

	/**
	 * 사용자의 등급 이름을 반환합니다.
	 * 등급이 없는 경우 "등급 없음"을 반환합니다.
	 * 
	 * @return 등급 이름 또는 "등급 없음"
	 */
	public String getGradeName() {
		return getGradeOptional()
			.map(Grade::getGrade)
			.orElse("등급 없음");
	}

	/**
	 * 사용자의 수수료율을 반환합니다.
	 * 등급이 없는 경우 0.0을 반환합니다.
	 * 
	 * @return 수수료율 또는 0.0
	 */
	public BigDecimal getFeeRate() {
		return getGradeOptional()
			.map(Grade::getFeeRate)
			.orElse(BigDecimal.ZERO);
	}
}
