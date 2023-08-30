package org.shaktifdn.registration.repository;

import org.shaktifdn.registration.model.UserRegisterStateDetail;
import org.springframework.data.couchbase.repository.ReactiveCouchbaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRegisterStateDetailRepository extends ReactiveCouchbaseRepository<UserRegisterStateDetail, String> {

}