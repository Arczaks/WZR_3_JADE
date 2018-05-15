/*
 *  Klasa agenta sprzedawcy książek.
 *  Sprzedawca dysponuje katalogiem książek oraz dwoma klasami zachowań:
 *  - OfferRequestsServer - obsługa odpowiedzi na oferty klientów
 *  - PurchaseOrdersServer - obsługa zamówienia klienta
 *
 *  Argumenty projektu (NETBEANS: project properties/run/arguments):
 *  -agents seller1:BookSellerAgent();seller2:BookSellerAgent();buyer1:BookBuyerAgent(Zamek) -gui
 */
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import java.util.*;


public class BookSellerAgent extends Agent
{
  // Katalog książek na sprzedaż:
  private Hashtable catalogue;

  // Inicjalizacja klasy agenta:
  @Override
  protected void setup()
  {
    // Tworzenie katalogu książek jako tablicy rozproszonej
    catalogue = new Hashtable();

    Random randomGenerator = new Random();    // generator liczb losowych

    catalogue.put("Zamek", 300+randomGenerator.nextInt(200));       // nazwa książki jako klucz, cena jako wartość
    catalogue.put("Proces", 250+randomGenerator.nextInt(70));
    catalogue.put("Opowiadania", 110+randomGenerator.nextInt(50));
    catalogue.put("Wirtualny swiat 401", 60+randomGenerator.nextInt(70));
    catalogue.put("W samo poludnie", 250+randomGenerator.nextInt(80));

    doWait(2000);                     // czekaj 2 sekundy

    System.out.println("Witam! Agent-sprzedawca (wersja c 2017/18) "+getAID().getName()+" jest gotów do handlu!");

    // Dodanie zachowania obsługującego odpowiedzi na oferty klientów (kupujących książki):
    addBehaviour(new OfferRequestsServer());

    // Dodanie zachowania obsługującego zamówienie klienta:
    addBehaviour(new PurchaseOrdersServer());
  }

  // Metoda realizująca zakończenie pracy agenta:
  @Override
  protected void takeDown()
  {
    System.out.println("Agent-sprzedawca (wersja c 2017/18) "+getAID().getName()+" zakończył działalność.");
  }


  /**
    Inner class OfferRequestsServer.
    This is the behaviour used by Book-seller agents to serve incoming requests
    for offer from buyer agents.
    If the requested book is in the local catalogue the seller agent replies
    with a PROPOSE message specifying the price. Otherwise a REFUSE message is
    sent back.
    */
    class OfferRequestsServer extends CyclicBehaviour
    {
        private Integer step = 0;
        private Integer lastPrice = -1;
        @Override
        public void action() {
            // Tworzenie szablonu wiadomości (wstępne określenie tego, co powinna zawierać wiadomość)
            System.out.println("Agen-sprzedawca AKCJA");
            ACLMessage msg, msg2;
            if (step == 0){
                System.out.println(step);
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);  
                msg = myAgent.receive(mt);
                msg2 = null;
            } else {
                System.out.println(step);
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                msg2 = myAgent.receive();
                msg = null;
            }

            if (msg != null || msg2 != null){
                System.out.println("Agent-sprzedawca cos odebrano");
                if (msg != null) {  // jeśli nadeszła wiadomość zgodna z ustalonym wcześniej szablonem
                    System.out.println("1");
                    String title = msg.getContent();  // odczytanie tytułu zamawianej książki
                    System.out.println("Agent-sprzedawca "+getAID().getName()+" otrzymał wiadomość: "+ title);
                    ACLMessage reply = msg.createReply();               // tworzenie wiadomości - odpowiedzi
                    Integer price = (Integer) catalogue.get(title);     // ustalenie ceny dla podanego tytułu
                    lastPrice = price;
                    if (price != null) {                                // jeśli taki tytuł jest dostępny
                        reply.setPerformative(ACLMessage.PROPOSE);            // ustalenie typu wiadomości (propozycja)
                        reply.setContent(String.valueOf(price.intValue()));   // umieszczenie ceny w polu zawartości (content)
                        System.out.println("Agent-sprzedawca "+getAID().getName()+" odpowiada: "+ price);
                    }
                    step = 1;
                    myAgent.send(reply);
                } else {  
                    if (msg2 != null){
                        System.out.println("2");
                        Integer newPrice = Integer.valueOf(msg2.getContent());
                        System.out.println("Agent-sprzedawca "+getAID().getName()+" otrzymał wiadomość: "+ newPrice);
                        ACLMessage reply = msg2.createReply(); 
                        Integer cenaDoWyslania = (int) ((newPrice + lastPrice) / 2.0);
                        if (cenaDoWyslania != null) {                                // jeśli taki tytuł jest dostępny
                            reply.setPerformative(ACLMessage.PROPOSE);            // ustalenie typu wiadomości (propozycja)
                            reply.setContent(String.valueOf(cenaDoWyslania.intValue()));   // umieszczenie ceny w polu zawartości (content)
                            System.out.println("Agent-sprzedawca "+getAID().getName()+" odpowiada: "+ cenaDoWyslania);
                        } 
                        myAgent.send(reply);
                } else {
                    System.out.println("3");
                    ACLMessage reply = msg.createReply();
                    // jeśli tytuł niedostępny
                    // The requested book is NOT available for sale.
                    reply.setPerformative(ACLMessage.REFUSE);         // ustalenie typu wiadomości (odmowa)
                    reply.setContent("tytuł niestety niedostępny");   
                    myAgent.send(reply);
                }
              }
            } else {                    // jeśli wiadomość nie nadeszła, lub była niezgodna z szablonem{
                System.out.println("Agent-sprzedawca: " + this.myAgent.getAID().getName() + " czeka");
                block();                 // blokada metody action() dopóki nie pojawi się nowa wiadomość
            }
        }
    } // Koniec klasy wewnętrznej będącej rozszerzeniem klasy CyclicBehaviour


    class PurchaseOrdersServer extends CyclicBehaviour
    {
      @Override
      public void action()
      {
        ACLMessage msg = myAgent.receive();

        if ((msg != null)&&(msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL))
        {
          // Message received. Process it          
          ACLMessage reply = msg.createReply();
          String title = msg.getContent();
          reply.setPerformative(ACLMessage.INFORM);
          System.out.println("Agent-sprzedawca (wersja c 2017/18) "+getAID().getName()+" sprzedał tytuł: "+title);
          myAgent.send(reply);
        }
      }
    } // Koniec klasy wewnętrznej będącej rozszerzeniem klasy CyclicBehaviour
} // Koniec klasy będącej rozszerzeniem klasy Agent