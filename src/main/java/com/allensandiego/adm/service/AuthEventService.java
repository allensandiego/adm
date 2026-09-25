package com.allensandiego.adm.service;

import com.allensandiego.adm.domain.AuthEvent;
import com.allensandiego.adm.domain.AuthOutcome;
import com.allensandiego.adm.repository.AuthEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthEventService {

    private final AuthEventRepository authEventRepository;

    @Transactional
    public void record(AuthEvent event) {
        authEventRepository.save(event);
    }

    public int countFailuresAfter(String username, java.time.LocalDateTime since) {
        return authEventRepository.countByUsernameAndOutcomeAfter(username, AuthOutcome.FAILURE, since);
    }
}
