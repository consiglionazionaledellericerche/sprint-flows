package it.cnr.si.flows.ng.service;

import static org.junit.Assert.assertEquals;

import java.util.stream.IntStream;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import it.cnr.si.FlowsApp;
import it.cnr.si.SprintApp;

@Ignore // Questo test non ha senso eseguirlo con H2, ma solo in locale con un postgres
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FlowsApp.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@EnableTransactionManagement
public class CounterServiceTest {

    @Autowired
    private CounterService counter;

    /**
         Per testare il corretto funzionamento, aggiungere nel'application.yml
    datasource:
        type: com.zaxxer.hikari.HikariDataSource
        url: jdbc:postgresql://localhost:5432/formazione
        name:
        username: formazione
        password: formazionepw
    jpa:
        database-platform: it.cnr.si.domain.util.FixedPostgreSQL82Dialect
        database: POSTGRESQL
        open-in-view: false
        show-sql: true
     */
    @Test
    public void testIncrement() throws InterruptedException {

        System.out.println(counter.getNext("pippo"));
        System.out.println(counter.getNext("pippo"));
        System.out.println(counter.getNext("pippo"));
        System.out.println(counter.getNext("pluto"));
        System.out.println(counter.getNext("paperino"));
        System.out.println(counter.getNext("pluto"));
        System.out.println(counter.getNext("pippo"));
        System.out.println(counter.getNext("pippo"));
        System.out.println(counter.getNext("pluto"));
        System.out.println(counter.getNext("paperino"));
        System.out.println(counter.getNext("pluto"));
        System.out.println(counter.getNext("pippo"));
        System.out.println(counter.getNext("pippo"));
        System.out.println(counter.getNext("pluto"));
        System.out.println(counter.getNext("paperino"));
        System.out.println(counter.getNext("pluto"));
        System.out.println(counter.getNext("pippo"));
        System.out.println(counter.getNext("caio"));
        counter.getNext("sempronio");

        counter.getNext("");
        counter.getNext("");
        counter.getNext("");
        counter.getNext("");

        try {
            long starting = counter.getNext("sempronio");
            IntStream.range(0, 10000).parallel().forEach(i -> {counter.getNext("sempronio");});
            assertEquals(counter.getNext("sempronio"), starting + 10001);
        } finally {
            counter.findAll().stream().forEach(System.out::println);
        }
    }

}
