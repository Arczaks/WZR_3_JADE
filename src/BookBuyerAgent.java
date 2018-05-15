/*
 *  Klasa agenta kupującego książki
 *
 *  Argumenty projektu (NETBEANS: project properties/run/arguments):
 *  -agents seller1:BookSellerAgent();seller2:BookSellerAgent();buyer1:BookBuyerAgent(Zamek) -gui
 */
        
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.*;

// Przykładowa klasa zachowania:
class MyOwnBehaviour extends Behaviour
{
  protected MyOwnBehaviour()
  {
  }
  @Override
  public void action()
  {
  }
  @Override
  public boolean done() {
    return false;
  }
}

public class BookBuyerAgent extends Agent {
    
    private static final int obnizanaWartosc = 6;

    private String targetBookTitle;    // tytuł kupowanej książki przekazywany poprzez argument wejściowy
    // lista znanych agentów sprzedających książki (w przypadku użycia żółtej księgi - usługi katalogowej, sprzedawcy
    // mogą być dołączani do listy dynamicznie!
    private AID[] sellerAgents = {
      new AID("seller1", AID.ISLOCALNAME),
      new AID("seller2", AID.ISLOCALNAME)};
    
    // Inicjalizacja klasy agenta:
    @Override
    protected void setup()
    {
     
      //doWait(5100);   // Oczekiwanie na uruchomienie agentów sprzedających

      System.out.println("Witam! Agent-kupiec "+getAID().getName()+" (wersja c 2017/18) jest gotów!");

      Object[] args = getArguments();  // lista argumentów wejściowych (tytuł książki)

      if (args != null && args.length > 0)   // jeśli podano tytuł książki
      {
        targetBookTitle = (String) args[0];
        System.out.println("Zamierzam kupić książkę zatytułowaną "+targetBookTitle);

        addBehaviour(new RequestPerformer());  // dodanie głównej klasy zachowań - kod znajduje się poniżej
       
      }
      else
      {
        // Jeśli nie przekazano poprzez argument tytułu książki, agent kończy działanie:
        System.out.println("Należy podać tytuł książki w argumentach wejściowych kupca!");
        doDelete();
      }
    }
    // Metoda realizująca zakończenie pracy agenta:
    @Override
    protected void takeDown()
    {
      System.out.println("Agent-kupiec "+getAID().getName()+" kończy istnienie.");
    }

    /**
    Inner class RequestPerformer.
    This is the behaviour used by Book-buyer agents to request seller
    agents the target book.
    */
    private class RequestPerformer extends Behaviour
    {
       
      private AID bestSeller;     // agent sprzedający z najkorzystniejszą ofertą
      private int bestPrice;      // najlepsza cena
      private int repliesCnt = 0; // liczba odpowiedzi od agentów
      private MessageTemplate mt; // szablon odpowiedzi
      private int step = 0;       // krok
      private int lastPrice = -1;
      private int licznik = 0;

      @Override
      public void action() {
        switch (step) {
            case 0:      // wysłanie oferty kupna
            System.out.print(" Oferta kupna (CFP) jest wysyłana do: ");
            for (int i = 0; i < sellerAgents.length; ++i) {
                System.out.print(sellerAgents[i]+ " ");
            }
            System.out.println();

            // Tworzenie wiadomości CFP do wszystkich sprzedawców:
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            for (int i = 0; i < sellerAgents.length; ++i) {
                cfp.addReceiver(sellerAgents[i]);          // dodanie adresate
            }
            cfp.setContent(targetBookTitle);             // wpisanie zawartości - tytułu książki
            cfp.setConversationId("book-trade");         // wpisanie specjalnego identyfikatora korespondencji
            cfp.setReplyWith("cfp"+System.currentTimeMillis()); // dodatkowa unikatowa wartość, żeby w razie odpowiedzi zidentyfikować adresatów
            myAgent.send(cfp);                           // wysłanie wiadomości
            // Utworzenie szablonu do odbioru ofert sprzedaży tylko od wskazanych sprzedawców:
            mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
                MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
            step = 1;     // przejście do kolejnego kroku
            break;
        case 1:      // odbiór ofert sprzedaży/odmowy od agentów-sprzedawców
            ACLMessage reply = myAgent.receive(mt);      // odbiór odpowiedzi
            if (reply != null) {
                if (reply.getPerformative() == ACLMessage.PROPOSE) {   // jeśli wiadomość jest typu PROPOSE
                    System.out.println("Agent-kupiec otrzymal odpowiedz, nowa cena: " + Integer.parseInt(reply.getContent()));
                    int price = Integer.parseInt(reply.getContent());  // cena książki
                    if (bestSeller == null || price < bestPrice) {       // jeśli jest to najlepsza oferta
                        bestPrice = price;
                        bestSeller = reply.getSender();
                    }
                }
                repliesCnt++;                                        // liczba ofert
                if (repliesCnt >= sellerAgents.length){               // jeśli liczba ofert co najmniej liczbie sprzedawców
                    if ((((bestPrice - lastPrice) > 3)) || (licznik != 8)){
                        System.out.println("Agent-kupiec przechodze do etapu 5");
                        step = 5;
                    } else {
                        System.out.println("Agent-kupiec przechodze do etapu 2");
                        step = 2;
                    }
                    lastPrice = bestPrice;
                }
            } else {
                block();
            }
            break;
        case 2:      // wysłanie zamówienia do sprzedawcy, który złożył najlepszą ofertę
            ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            order.addReceiver(bestSeller);
            order.setContent(targetBookTitle);
            order.setConversationId("book-trade");
            order.setReplyWith("order"+System.currentTimeMillis());
            myAgent.send(order);
            mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
                MessageTemplate.MatchInReplyTo(order.getReplyWith()));
            step = 3;
            break;
        case 3:      // odbiór odpowiedzi na zamównienie
            reply = myAgent.receive(mt);
            if (reply != null) {
              if (reply.getPerformative() == ACLMessage.INFORM) {
                System.out.println("Tytuł "+targetBookTitle+" został zamówiony.");
                System.out.println("Cena = "+bestPrice);
                myAgent.doDelete();
              }
              step = 4;
            } else {
              block();
            }
            break;
        case 5: //zmiana ceny
            //  System.out.println("Zaczynam negocjacje");
           ACLMessage order2 = new ACLMessage(ACLMessage.INFORM);
           order2.addReceiver(bestSeller);
           int cena;
           if (licznik == 0){
               cena = (int)(bestPrice * 0.7);
           } else {
               cena = bestPrice - obnizanaWartosc;
           }
           licznik++;
           //  System.out.println("cena: " + cena + "licznik: " + licznik);
           order2.setContent(String.valueOf(cena));
           order2.setConversationId("book-trade");
           order2.addReceiver(bestSeller);
           order2.setReplyWith("order"+System.currentTimeMillis());
           myAgent.send(order2);
           mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
             MessageTemplate.MatchInReplyTo(order2.getReplyWith()));
           System.out.println("Agent-kupiec wysylam nowa cene: " + cena + " do: " + bestSeller);
           step = 1;
           break;
        }  // switch
      } // action

      @Override
      public boolean done() {
        return ((step == 2 && bestSeller == null) || step == 4);
      }
    } // Koniec wewnętrznej klasy RequestPerformer
}
