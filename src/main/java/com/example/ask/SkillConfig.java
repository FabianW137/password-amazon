package com.example.ask;

import com.amazon.ask.Skill;
import com.amazon.ask.Skills;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SkillConfig {

  @Bean
  public Skill skill(
          @Value("${app.skill-id}") String skillId,
          LaunchRequestHandler launch,
          LinkIntentHandler link,
          AuthenticateIntentHandler auth,
          CancelAndStopHandler cancelStop,
          FallbackHandler fallback,
          SessionEndedHandler ended
  ) {
    return Skills.standard()
            .withSkillId(skillId)                // aktiviert ApplicationId-Prüfung
            .addRequestHandlers(
                    launch, link, auth, cancelStop, fallback, ended
            )
            .build();
  }
}
