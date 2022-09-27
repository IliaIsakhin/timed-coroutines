package ilia.isakhin.timed.coroutines.controller

import ilia.isakhin.timed.coroutines.aspect.TimedCoroutine
import kotlinx.coroutines.delay
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MyController {
    
    @Autowired
    private lateinit var myService: MyService
    
    @GetMapping
    suspend fun getUsers(): List<String> {
        return myService.getUsers()
    }
}

@Service
class MyService {

    @Autowired
    private lateinit var myRepository: MyRepository
    
    @TimedCoroutine
    suspend fun getUsers(): List<String> {
        return myRepository.getUsers()   
    }
}

@Repository
class MyRepository {
    
    suspend fun getUsers(): List<String> {
        delay(1000L)
        
        return emptyList()
    }
    
}
