package pt.ul.fc.mas.aggregator;

import com.google.gson.Gson;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import pt.ul.fc.mas.aggregator.model.News;
import pt.ul.fc.mas.aggregator.model.NewsSearchResult;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

public class AggregatorBehaviour extends ContractNetInitiator {
    private int nResponders;

    public AggregatorBehaviour(Agent a, ACLMessage cfp) {
        super(a, cfp);
    }

    @Override
    protected void handlePropose(ACLMessage propose, Vector v) {
        System.out.println("Agent " + propose.getSender().getName() + " proposed " + propose.getContent());
    }

    @Override
    protected void handleRefuse(ACLMessage refuse) {
        System.out.println("Agent " + refuse.getSender().getName() + " refused");
    }

    @Override
    protected void handleFailure(ACLMessage failure) {
        if (failure.getSender().equals(myAgent.getAMS())) {
            // FAILURE notification from the JADE runtime: the receiver
            // does not exist
            System.out.println("Responder does not exist");
        } else {
            System.out.println("Agent " + failure.getSender().getName() + " failed");
        }
        // Immediate failure --> we will not receive a response from this agent
        nResponders--;
    }

    @Override
    protected void handleAllResponses(Vector responses, Vector acceptances) {
        if (responses.size() < nResponders) {
            // Some responder didn't reply within the specified timeout
            System.out.println("Timeout expired: missing " + (nResponders - responses.size()) + " responses");
        }
        // Evaluate proposals.
        Enumeration e = responses.elements();
        while (e.hasMoreElements()) {
            ACLMessage msg = (ACLMessage) e.nextElement();
            if (msg.getPerformative() == ACLMessage.PROPOSE) {
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                acceptances.addElement(reply);
                System.out.println("Agent " + msg.getSender().getName() + " proposed: " + msg.getContent());
            } else if (msg.getPerformative() == ACLMessage.REFUSE) {
                System.out.println("Agent " + msg.getSender().getName() + " refused: " + msg.getContent());
            }
        }
    }

    @Override
    protected void handleInform(ACLMessage inform) {
        System.out.println("Agent " + inform.getSender().getName() + " successfully performed the requested action");
    }

    @Override
    protected void handleAllResultNotifications(Vector resultNotifications) {
        ArrayList<News> results = new ArrayList<>();
        Gson gson = new Gson();

        // Evaluate proposals.
        Enumeration e = resultNotifications.elements();
        while (e.hasMoreElements()) {
            ACLMessage msg = (ACLMessage) e.nextElement();
            if (msg.getPerformative() == ACLMessage.INFORM) {
                NewsSearchResult searchResult = gson.fromJson(msg.getContent(), NewsSearchResult.class);
                System.out.println("Agent " + msg.getSender().getName() + " found: " + searchResult.getResults().size() + " news.");
                if (searchResult.getResults().size() > 0) {
                    results.addAll(searchResult.getResults());
                }
            } else if (msg.getPerformative() == ACLMessage.FAILURE) {
                System.out.println("Agent " + msg.getSender().getName() + " failed: " + msg.getContent());
            }
        }

        // TODO: handle displaying the news. Send to Presenter agent?
        NewsSearchResult newsSearch = new NewsSearchResult(results);
        System.out.println("Found news:");
        for (News result : results) {
            System.out.println(result.getContent());
            System.out.println();
        }
    }
}