package by.mog.avinahradau.mogilev_audio_quiz.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LevelCondition {
    private String photoHelp1;
    private String textHelp1;
    private String photoHelp2;
    private String textHelp2;
    private String photoHelp3;
    private String textHelp3;
    private String levelProgress;
    private String answer;
    private String correctMessage;
    private String info;
    private String audioFolderName;
    private String progressLetter;
    private String nextLevelTxt;
}
