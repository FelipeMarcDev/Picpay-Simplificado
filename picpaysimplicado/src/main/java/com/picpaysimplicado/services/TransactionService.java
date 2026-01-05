package com.picpaysimplicado.services;

import com.picpaysimplicado.domain.transaction.Transaction;
import com.picpaysimplicado.domain.user.User;
import com.picpaysimplicado.dtos.TransactionDTO;
import com.picpaysimplicado.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
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

        boolean isAuthorized = this.authorizeTransaction(sender, transaction.value());
        if (!isAuthorized){
            throw new Exception("Transação não autorizada");
        }

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

        this.notificationService.sendNotification(receiver, "Transação recebido com sucesso");

        this.notificationService.sendNotification(receiver, "Transação realizada com sucesso");

        return newTransaction;
    }
    public boolean authorizeTransaction(User sender, BigDecimal value){
       ResponseEntity<Map> authroizationResponse = restTemplate.getForEntity("https://util.devi.tools/api/v2/authorize", Map.class);

        if(authroizationResponse.getStatusCode() == HttpStatus.OK) {
            String message = authroizationResponse.getBody().get("authorization").toString();
            return "true".equals(message);} else return false;
        }

    public List<Transaction> getAllTransactions(){
        return this.repository.findAll();
    }
}

