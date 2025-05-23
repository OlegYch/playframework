/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.forms;

// #register-formatter
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.text.ParseException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import play.data.format.Formatters;
import play.data.format.Formatters.SimpleFormatter;
import play.i18n.MessagesApi;

@Singleton
public class FormattersProvider implements Provider<Formatters> {

  private final MessagesApi messagesApi;

  @Inject
  public FormattersProvider(MessagesApi messagesApi) {
    this.messagesApi = messagesApi;
  }

  @Override
  public Formatters get() {
    Formatters formatters = new Formatters(messagesApi);

    formatters.register(
        LocalTime.class,
        new SimpleFormatter<LocalTime>() {

          private Pattern timePattern = Pattern.compile("([012]?\\d)(?:[\\s:\\._\\-]+([0-5]\\d))?");

          @Override
          public LocalTime parse(String input, Locale l) throws ParseException {
            Matcher m = timePattern.matcher(input);
            if (!m.find()) throw new ParseException("No valid Input", 0);
            int hour = Integer.valueOf(m.group(1));
            int min = m.group(2) == null ? 0 : Integer.valueOf(m.group(2));
            return LocalTime.of(hour, min);
          }

          @Override
          public String print(LocalTime localTime, Locale l) {
            return localTime.format(DateTimeFormatter.ofPattern("HH:mm"));
          }
        });

    return formatters;
  }
}
// #register-formatter
