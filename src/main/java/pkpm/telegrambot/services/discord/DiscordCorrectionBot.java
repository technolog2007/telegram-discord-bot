package pkpm.telegrambot.services.discord;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

public class DiscordCorrectionBot extends ListenerAdapter {

  private static final Logger log = LoggerFactory.getLogger(DiscordCorrectionBot.class);

  public static void main(String[] args) {
    String token = System.getenv("BOT_TOKEN_DISCORD");

    if (token == null || token.isEmpty()) {
      log.error("Discord bot token not found. Please set DISCORD_BOT_TOKEN environment variable.");
      return;
    }

    // Вказуємо інтенти, які потрібні боту
    EnumSet<GatewayIntent> intents = EnumSet.of(
        GatewayIntent.GUILD_MESSAGES,          // Для отримання повідомлень з гільдій (серверів)
        GatewayIntent.MESSAGE_CONTENT,         // Для читання вмісту повідомлень (ОБОВ'ЯЗКОВО!)
        GatewayIntent.GUILD_MEMBERS,           // Для доступу до списку учасників гільдій
        GatewayIntent.GUILD_MESSAGE_REACTIONS  // Якщо плануєте працювати з реакціями
    );

    JDABuilder.createDefault(token)
        .setActivity(Activity.listening("твої повідомлення")) // Статус бота
        .enableIntents(intents) // Включаємо інтенти
        // Деякі налаштування кешу та політик, які можуть бути корисні
        .setMemberCachePolicy(MemberCachePolicy.ALL)
        .setChunkingFilter(ChunkingFilter.ALL)
        .enableCache(EnumSet.of(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE))
        .addEventListeners(new DiscordCorrectionBot()) // Додаємо наш обробник подій
        .build(); // Будуємо і підключаємо бота до Discord
  }

  @Override
  public void onMessageReceived(MessageReceivedEvent event) {
    // Ігноруємо повідомлення від самого бота, щоб уникнути нескінченних циклів
    if (event.getAuthor().isBot()) {
      return;
    }

    String inputMessage = event.getMessage().getContentRaw(); // Отримуємо сирий текст повідомлення
    long channelId = event.getChannel().getIdLong(); // ID каналу, з якого прийшло повідомлення
    String authorName = event.getAuthor().getAsTag(); // Ім'я автора (наприклад, User#1234)

    log.info("Received message from {}: {}", authorName, inputMessage);

    // ==== Логіка аналізу та виправлення повідомлення ====
    String correctedMessage = analyzeAndCorrectSyntax(inputMessage);

    if (!inputMessage.equals(correctedMessage)) {
      log.info("Correcting message from {}: Original: '{}', Corrected: '{}'", authorName,
          inputMessage, correctedMessage);

      // Спроба видалити оригінальне повідомлення
      event.getMessage().delete().queue(
          // Успішне видалення: тепер відправляємо виправлене повідомлення
          (Void) -> { // 'Void' тому що delete() нічого не повертає при успіху
            event.getChannel().sendMessage(
                "Повідомлення від " + event.getAuthor().getAsMention()
                    + ": \n" + correctedMessage).queue(
                (message) -> log.info("Successfully sent corrected message in channel {}.",
                    channelId),
                (error) -> log.error(
                    "Failed to send corrected message after deletion in channel {}: {}", channelId,
                    error.getMessage())
            );
          },
          // Помилка видалення (наприклад, немає дозволів): тоді просто відправляємо нове повідомлення
          (error) -> {
            log.error("Failed to delete original message from {}: {}", authorName,
                error.getMessage());
            // Відправляємо нове виправлене повідомлення, бо не вдалося видалити оригінал
            event.getChannel().sendMessage(
                    "Виправлення для " + event.getAuthor().getAsMention() + ": " + correctedMessage)
                .queue(
                    (message) -> log.info(
                        "Successfully sent corrected message (original not deleted) in channel {}.",
                        channelId),
                    (error2) -> log.error(
                        "Failed to send corrected message (original not deleted) in channel {}: {}",
                        channelId, error2.getMessage())
                );
          }
      );
    } else {
      log.info("Message from {} is correct: '{}'", authorName, inputMessage);
    }
  }

  /**
   * Метод заміняє "_" на "\_" в тексті повідомлення
   *
   * @param text Оригінальний текст повідомлення.
   * @return Виправлений текст повідомлення.
   */
  private String analyzeAndCorrectSyntax(String text) {
    String corrected = text;

    if (corrected.toLowerCase().contains("_") || corrected.toLowerCase().contains("\\")) {
      corrected = corrected.replace("\\", "\\\\" ).replace("_", "\\_");
    }
    return corrected;
  }
}
