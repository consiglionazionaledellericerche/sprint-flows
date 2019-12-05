## ASPETTI TECNOLOGICI

Il software è sviluppato come una web application che può funzionare in modalità standalone, cioè anche senza un application server (es. Tomcat) dedicato.

Il software è stato sviluppato con il framework JHipster, che comprende le seguenti tecnologie:

- **Spring Boot** per IOC e web server
- **Liquibase** per la persistenza dei dati, l'applicazione può usare diversi DMBS (es. H2 per i tests, PostgreSQL per produzione)
- **Hibernate** per le cache e le configuazioni distribuite; l'applicazione può avere diverse istanze per la ridondanza e gestione del carico
- **AngularJS** per le interfacce
- **Activiti** per la gestione dei processi
- **Spring Cloud Config** per la confiurazione esternalizzata
- **Sprint Storage Cloud** per la gestione dei documenti

Le funzionalità sviluppato a supporto dell'esecuzione dei processi amministrativi sono:

- **Gestione dell'autenticazione** che può avvenire attraverso utenze locali all'applicazione, attraverso LDAP oppure attraverso ulteriori gestori di utenze (es. ACE, da aggiungere ad-hoc)
- **Gestione delle autorizzazioni** sia in locale che attraverse gestori esterni (es. ACE)
- **Gestione dei metadati** per ogni processo, compresi gli allegati, e la cronologia dettagliata (chi ha fatto cosa e quando)
- **Gestione dei compiti e delle visibilità** dei flussi amministrativi
- **Notifiche email** sia predefinite (es. "hai un nuovo compito") che personalizzate (es. "Il tuo compito è in attesa da X giorni")
- **Gestione Firma Digitale** interna ai flussi amministrativi (richiede sign-server esterno)
- **Comunicazione con altre applicazioni** attraverso interfacce REST (altre applicazioni possono eseguire compiti in Scrivania ed eventi in Scrivania possono chiamare REST di altre applicazioni)
- **Azioni custom** per ogni tipo di evento (da sviluppare ad hoc)
- **Generazione report e statistiche** in .pdf e in .csv