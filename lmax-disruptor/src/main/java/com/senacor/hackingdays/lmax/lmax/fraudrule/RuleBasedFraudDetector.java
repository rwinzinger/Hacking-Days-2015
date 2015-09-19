package com.senacor.hackingdays.lmax.lmax.fraudrule;

import com.senacor.hackingdays.lmax.generate.model.Profile;
import com.senacor.hackingdays.lmax.lmax.CompletableConsumer;
import com.senacor.hackingdays.lmax.lmax.fraud.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Created with IntelliJ IDEA.
 * User: lrscholz
 * Date: 19/09/15
 * Time: 19:09
 * To change this template use File | Settings | File Templates.
 */
public class RuleBasedFraudDetector extends CompletableConsumer {

  public RuleBasedFraudDetector(int expectedMessages, Runnable onComplete) {
    super(expectedMessages, onComplete);
  }

  boolean verbose = false;

  List<FraudRule> fraudRules = new ArrayList<>();

  {
    fraudRules.add(new FraudRule(p -> p.getAge() < 18, "users must be of full age"));
    fraudRules.add(new FraudRule(p -> p.getActivity().getLoginCount() > 100, "potential attacker"));
    fraudRules.add(new FraudRule(p -> p.getSeeking().getAgeRange().getLower() < 16, "pedophile"));
  }

  @Override
  protected void onComplete() {

    long count = fraudRules.parallelStream().mapToLong(FraudRule::getCount).sum();

    System.out.println("Detected " + count + " fraud attempts");

    if (verbose) {
      for(FraudRule r: fraudRules) {
        for(Profile profile: r.getDetected()) {
          String violation = r.getMessage();

          String text = MessageFormat.format(" name {0}, age {1}, gender {2}, state {3}, city {4}, gender {5}, relationship {6}, seeking {7}", profile.getName(), profile.getAge(), profile.getGender().toString(), profile.getLocation().getState(), profile.getLocation().getCity(), profile.getGender().toString(), profile.getRelationShip().toString(), profile.getSeeking().getAgeRange().getLower());
          System.out.println(violation + ": " + text);
        }
      }
    }
  }

  private List<Profile> batch = new ArrayList<>();

  @Override
  protected void processEvent(Profile profile, long sequence, boolean endOfBatch) {

    if (!endOfBatch) {
      batch.add(profile);
    }

    if (endOfBatch) {
      fraudRules.parallelStream().forEach(r -> {
        batch.stream().filter(p -> r.getRule().test(p)).forEach(p -> r.addDetected(p));
      });

      batch.clear();
    }
  }
}
