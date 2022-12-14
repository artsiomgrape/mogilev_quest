package by.mog.avinahradau.mogilev_audio_quiz.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import by.mog.avinahradau.mogilev_audio_quiz.config.BotConfig;
import by.mog.avinahradau.mogilev_audio_quiz.model.LevelCondition;
import by.mog.avinahradau.mogilev_audio_quiz.model.LevelsDB;
import by.mog.avinahradau.mogilev_audio_quiz.model.UserProgress;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig botConfig;
    private final LevelsDB levelsDB;

    private Map<Long, UserProgress> userProgressDB = new HashMap();

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            UserProgress progress = userProgressDB.get(chatId);
            if (progress == null) {
                progress = new UserProgress();
                userProgressDB.put(chatId, progress);
            }

            if (messageText.equals("/start")) {
                task(chatId,
                     "<b>1 ????????. ?????? ????????????????\n"
                     + "?????????????? ??????????, ?? ?????????????? ???????? ???????? ?? ???????????????????????????? ????????????????: ?????????? 1526 ?????????????? ??? ?????????????? ?? ?????????????? ?????????? ???????? ????????????, ???? ?????????????? ?????????????????\n\n???????????????? ?????????????????????? ??????????</b>");
            }

            for (LevelCondition level : levelsDB.getLevels()) {
                addLevel(chatId, progress, messageText, level);

            }
            if (messageText.equals("???????????????????? ????????????????")) {
                if (Objects.isNull(progress.getProgress())) {
                    sendMessage(chatId, "???? ?????? ???? ???????????? ???? ???????????? ??????????????");
                } else {
                    sendMessage(chatId, progress.getProgress());
                }
            }
            if (messageText.trim().equalsIgnoreCase("????????????")) {
                userProgressDB.remove(chatId);
                sendMessage(chatId, "?????????????? ???? /start");
            }
        }
    }

    private void addLevel(long chatId, UserProgress progress, String messageText, LevelCondition level) {
        if (Objects.equals(progress.getProgress(), level.getLevelProgress())) {
            if (messageText.equals("?????????????????? 1")) {
                showHelp(chatId, level.getPhotoHelp1(), level.getTextHelp1());
            } else if (messageText.equals("?????????????????? 2")) {
                showHelp(chatId, level.getPhotoHelp2(), level.getTextHelp2());
            } else if (messageText.equals("?????????????????? 3")) {
                showHelp(chatId, level.getPhotoHelp3(), level.getTextHelp3());
            } else {
                if (messageText.toLowerCase().trim().equals(level.getAnswer())) {
                    sendMessage(chatId, level.getCorrectMessage());
                    String info = level.getInfo();
                    int mid = info.length() / 2; //get the middle of the String
                    String[] parts = {info.substring(0, mid), info.substring(mid)};

                    sendMessage(chatId, parts[0]);
                    sendMessage(chatId, parts[1]);
                    sendAudio(chatId, level.getAudioFolderName());
                    String progressString = progress.getProgress();
                    if (progressString == null) {
                        progress.setProgress(level.getProgressLetter());
                    } else {
                        progress.setProgress(progress.getProgress() + level.getProgressLetter());
                    }

                    task(chatId, level.getNextLevelTxt());
                } else {
                    if (!messageText.equals("???????????????????? ????????????????") && !messageText.equals("/start")
                        && !messageText.equalsIgnoreCase("????????????")
                        && !messageText.toLowerCase().trim().equals(levelsDB.getPreviousAnswer(level))) {
                        sendMessage(chatId, "?????????? ????????????????");
                    }
                }
            }
        }
    }

    private void sendAudio(long chatId, String task) {
        try {
            ResourceLoader resourceLoader = new DefaultResourceLoader();
            Resource resource = resourceLoader.getResource("classpath:" + task + "/audio/audio.mp3");
            try {
                InputStream inputStream = resource.getInputStream();
                SendAudio sendAudio = new SendAudio();
                sendAudio.setChatId(chatId);
                sendAudio.setAudio(new InputFile(inputStream, task + "/audio/audio.mp3"));
                execute(sendAudio);
            } catch (Exception e) {
                e.printStackTrace();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showHelp(long chatId, String photoPath, String helpTxt) {
        try {
            if (Objects.nonNull(photoPath)) {
                sendPhoto(chatId, photoPath);
            }
            if (Objects.nonNull(helpTxt)) {
                sendMessage(chatId, helpTxt);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendPhoto(long chatId, String photoPath) throws FileNotFoundException, TelegramApiException {
        SendPhoto message = new SendPhoto();
        message.setChatId(chatId);
        InputFile photo = new InputFile();

        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource("classpath:" + photoPath);
        try {
            InputStream inputStream = resource.getInputStream();

            photo.setMedia(inputStream, photoPath);
            message.setPhoto(photo);
            message.setReplyMarkup(getReplyKeyboardMarkup());
            execute(message);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void task(long chatId, String taskText) {
        ReplyKeyboardMarkup keyboardMarkup = getReplyKeyboardMarkup();

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setReplyMarkup(keyboardMarkup);
        sendMessage.setText(taskText);
        sendMessage.enableHtml(true);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private ReplyKeyboardMarkup getReplyKeyboardMarkup() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("?????????????????? 1");
        row.add("?????????????????? 2");
        row.add("?????????????????? 3");
        keyboard.add(row);

        row = new KeyboardRow();
        row.add("???????????????????? ????????????????");

        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private void sendMessage(long chatId, String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(message);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
