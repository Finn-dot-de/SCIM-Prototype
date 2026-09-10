package spring.and.scim.de.prototype.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ScimConfiguration {

    /**
     * Die Uhr als Bean, damit Zeitstempel eine explizite Abhaengigkeit sind und
     * in Tests durch {@code Clock.fixed(...)} ersetzt werden koennen — statt
     * ueber {@code Calendar.getInstance()} fest an der Systemzeit zu haengen.
     * SCIM-Zeitstempel sind laut RFC 7643 ISO-8601, daher UTC.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
