package dim.application.bot;

import dim.application.bot.component.Compliment;
import dim.application.bot.component.Divination;
import dim.application.bot.component.InlineKeyboardMaker;
import dim.application.bot.component.ReplyKeyboardMaker;
import dim.application.bot.component.Stickers;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Component
public class InlineMessageHandler {

    @Autowired
    private RoboCatBot bot;

    @Autowired
    private ReplyKeyboardMaker replyKeyboardMaker;

    @Autowired
    private InlineKeyboardMaker inlineKeyboardMaker;

    private Properties properties = new Properties();

    private Random randomGen = new Random();

    //private List<String> users;

    public String handleMessage(Update update) {

        String chatId;
        String message;
        String userName;

        if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId().toString();
            message = update.getCallbackQuery().getData();
            userName = update.getCallbackQuery().getFrom().getFirstName();
        } else {
            chatId = update.getMessage().getChatId().toString();
            message = update.getMessage().getText();
            userName = update.getMessage().getFrom().getFirstName();
        }

        if (Utils.checkBadWords(bot, message, chatId, update)) {
            return "";
        }

        switch (message) {
            case "/start":
            case "/start@robo_cat_bot":
                SendMessage badMessage2 = new SendMessage(chatId, userName + ", ?????? ?????????? ?????????????");
                badMessage2.enableMarkdown(true);
                badMessage2.setReplyMarkup(inlineKeyboardMaker.getInlineMessageButtons());
                bot.sendMessage(badMessage2);
                return "";

            case "?????????? ?????????????????? ??????????????": {
                if (bot.getMonth() == null || !bot.getMonth().equals(LocalDate.now().getMonth())) {
                    bot.setMonth(LocalDate.now().getMonth());
                    bot.getStatistic().clear();
                }
                if (bot.getDateForRooster() == null || !bot.getDateForRooster().isEqual(LocalDate.now())) {
                    bot.setDateForRooster(LocalDate.now());
                } else {
                    SendMessage sm = new SendMessage(chatId,
                            userName + ", ?????? ?????? ??????????, ?????? ?????????????? ?????????? \uD83D\uDE09");
                    bot.sendMessage(sm);
                    return "";
                }

                Utils.checkProperties(properties, update);

                int random = randomGen.nextInt(properties.size());
                List<String> list = new ArrayList<>();
                for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                    list.add(entry.getValue().toString());
                }

                bot.sendMessage(new SendMessage(chatId, "?????????? ?????????????????? ?????????????? ???\uD83D\uDC14"));

                estimationResponse(chatId);

                String newRooster = list.get(random);

                bot.getStatistic().put(newRooster, bot.getStatistic().getOrDefault(newRooster, 0) + 1);

                return list.isEmpty() ? "\uD83C\uDF89 ?????????????? ?????? ????????????" : "\uD83C\uDF89 ?????????????? ?????????? - " + newRooster;
            }

            case "???????????? ??????????????????":
                if (bot.getMonth() == null || !bot.getMonth().equals(LocalDate.now().getMonth()) || bot.getStatistic().isEmpty()) {
                    bot.setMonth(LocalDate.now().getMonth());
                    bot.getStatistic().clear();
                    return "?????????????????? ?????????????? ?? ???????? ???????????? ?????? ???? ??????????????";
                }

                StringBuilder stringBuilder = new StringBuilder();

                bot.getStatistic().entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .collect(Collectors.toList())
                        .forEach(v -> stringBuilder.append(v).append("\n"));

                bot.sendMessage(new SendMessage(chatId, userName + ", ?????? ???????? ?????????????????? ???????????????????? ???? ??????????:\n\n" + stringBuilder));
                return "";

            case "???????????? ??????-????????????":
                //SendAudio audioMessage = new SendAudio(chatId, new InputFile(new File("src/main/resources/audio/audio_2.mp3")));
                SendAudio audioMessage = new SendAudio(chatId, new InputFile(new File("target/classes/audio/audio_2.mp3")));
                try {
                    bot.execute(audioMessage);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                return "";

            case "?????????? ?????? ??????-???????????? ????????????????":
                List<String> compliments = new ArrayList<>();
                compliments.addAll(0, Compliment.compliments);
                Collections.shuffle(compliments);
                bot.sendMessage(new SendMessage(chatId, userName + ", " + compliments.get(0)));
                return "";

            //case "?? ???????? ????????????????":

            case "???????????? ?????? ????????????":
                bot.sendMessage(new SendMessage(chatId, userName + ", ?????? ???????? ?????????? \uD83D\uDC08:"));
                List<String> catsList = new ArrayList<>();
                catsList.addAll(0, Stickers.stickersList);
                Collections.shuffle(catsList);
                SendSticker stickerMessage = new SendSticker(chatId, new InputFile(catsList.get(0)));
                try {
                    bot.execute(stickerMessage);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
                return "";

            case "/stop":
            case "/stop@robo_cat_bot":
                return "???????? ????????! ???????????????? ??????, ????????????!";

            default:
                return "";
        }
    }

    public void estimationResponse(String chatId) {

        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        List<String> list = new ArrayList<>();
        list.addAll(0, Divination.divinationList);

        int random = randomGen.nextInt(2) + 3;
        Collections.shuffle(list);

        while (random >= 0) {
            SendMessage message = new SendMessage(chatId, list.get(random));
            bot.sendMessage(message);
            list.remove(random);
            random--;
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
}
