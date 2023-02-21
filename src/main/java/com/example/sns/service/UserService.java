package com.example.sns.service;

import com.example.sns.exception.ErrorCode;
import com.example.sns.exception.SnsApplicationException;
import com.example.sns.model.User;
import com.example.sns.model.entity.UserEntity;
import com.example.sns.repository.UserEntityRepository;
import com.example.sns.util.JwtTokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserEntityRepository userEntityRepository;
    private final BCryptPasswordEncoder encoder;

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.token.expired-time-ms}")
    private Long expiredTimeMs;

    @Transactional
    public User join(String userName, String password){
        // 회원가입하려는 userName으로 회원가입된 유저가 있는지 확인.
        userEntityRepository.findByUserName(userName).ifPresent(it -> {
            throw new SnsApplicationException(ErrorCode.DUPLICATED_USER_NAME, String.format("%s is duplicated", userName));
        });

        // 회원가입 진행 = 유저 등록.
        UserEntity userEntity = userEntityRepository.save(UserEntity.of(userName, encoder.encode(password)));
        return User.fromEntity(userEntity);
    }
    
    public String login(String userName, String password){
        // 회원가입 여부 확인.
        UserEntity userEntity = userEntityRepository.findByUserName(userName).orElseThrow(() -> new SnsApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s not founded", userName)));

        // 비밀번호 확인.
        if (!encoder.matches(password, userEntity.getPassword())){
            throw new SnsApplicationException(ErrorCode.INVALID_PASSWORD);
        }

        // 토큰 생성.
        String token = JwtTokenUtils.generateToken(userName, secretKey, expiredTimeMs);
        return token;
    }
}