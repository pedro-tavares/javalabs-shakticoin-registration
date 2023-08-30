package org.shaktifdn.registration.repository;

import org.shaktifdn.registration.model.UserRegisterState;
import org.shaktifdn.registration.model.UserRegisterStateDetail;
import org.springframework.data.couchbase.repository.Query;
import org.springframework.data.couchbase.repository.ReactiveCouchbaseRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
public interface UserRegisterStateRepository extends ReactiveCouchbaseRepository<UserRegisterState, String> {

    @Query("#{#n1ql.selectEntity} where `_type`=\"" + UserRegisterState.TYPE + "\" and `shaktiId` = $1 LIMIT 1")
    Mono<UserRegisterState> findByShaktiId(String shaktiId);

    /*
     Below query requires this index
     CREATE INDEX adv_userRegisterStateId_type ON `services`(`userRegisterStateId`) WHERE `_type` = 'UserRegisterStateDetail'
     */
    @Query("SELECT DISTINCT META(u).id AS __id, META(u).cas AS __cas, u.* FROM #{#n1ql.bucket} u " +
            " JOIN #{#n1ql.bucket} d ON META(u).id = d.userRegisterStateId AND d.`_type`=\"" + UserRegisterStateDetail.TYPE + "\" " +
            " WHERE u.`_type`=\"" + UserRegisterState.TYPE + "\"  AND u.`lastModification` < $1" +
            " GROUP BY u HAVING COUNT(DISTINCT d.stateType) < $2")
    Flux<UserRegisterState> findByIncomplete(Instant modifiedBefore, Integer countOfTypes);

}