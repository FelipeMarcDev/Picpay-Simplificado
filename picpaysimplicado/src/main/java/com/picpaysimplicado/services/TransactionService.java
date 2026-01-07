package com.picpaysimplicado.services;

import com.picpaysimplicado.domain.transaction.Transaction;
import com.picpaysimplicado.domain.user.User;
import com.picpaysimplicado.dtos.TransactionDTO;
import com.picpaysimplicado.infra.exceptions.TransactionNotAuthorizedException;
import com.picpaysimplicado.repositories.TransactionRepository;
import jakarta.transaction.TransactionalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class TransactionService {
    @Autowired
    private UserService userService;

    @Autowired
    private TransactionRepository repository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private NotificationService notificationService;

    public Transaction createTransaction(TransactionDTO transaction) throws Exception {
        User sender = this.userService.findUserById(transaction.senderId());
        User receiver = this.userService.findUserById(transaction.receiverId());

        userService.validateTransaction(sender,transaction.value());

        this.authorizeTransaction(sender,transaction.value());

        Transaction newTransaction = new Transaction();
        newTransaction.setAmount(transaction.value());
        newTransaction.setSender(sender);
        newTransaction.setReceiver(receiver);
        newTransaction.setTimestamp(LocalDateTime.now());

        sender.setBalance(sender.getBalance().subtract(transaction.value()));
        receiver.setBalance(receiver.getBalance().add(transaction.value()));

        this.repository.save(newTransaction);
        this.userService.saveUser(sender);
        this.userService.saveUser(receiver);

        this.notificationService.sendNotification(receiver, "Transação recebida com sucesso");

        this.notificationService.sendNotification(sender, "Transação enviada com sucesso");

        return newTransaction;
    }
    public void authorizeTransaction(User sender, BigDecimal value) {

        try {
            ResponseEntity<Map> response =
                    restTemplate.getForEntity(
                            "https://util.devi.tools/api/v2/authorize",
                            Map.class
                    );

            Map body = response.getBody();
            Map data = (Map) body.get("data");

            if (data == null || !Boolean.TRUE.equals(data.get("authorization"))) {
                throw new TransactionNotAuthorizedException();
            }

        } catch (HttpClientErrorException.Forbidden e) {
            // 403 vindo da API externa
            throw new TransactionNotAuthorizedException();
        }
    }


    public List<Transaction> getAllTransactions(){
        return this.repository.findAll();
    }
}

