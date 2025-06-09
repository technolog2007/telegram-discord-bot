package pkpm.telegrambot.models;

public class ButtonAction {

  private Buttons button;
  private boolean additionalDialogue = false;
  private String messageDate;
  private String replyButton = null;
  private String compositeMessage = "";


  public ButtonAction(){}

  public ButtonAction(Buttons button, boolean additionalDialogue, String messageDate,
      String replyButton, String compositeMessage) {
    this.button = button;
    this.additionalDialogue = additionalDialogue;
    this.messageDate = messageDate;
    this.replyButton = replyButton;
    this.compositeMessage = compositeMessage;
  }

  public Buttons getButton() {
    return button;
  }

  public ButtonAction setButton(Buttons button) {
    this.button = button;
    return this;
  }

  public boolean isAdditionalDialogue() {
    return additionalDialogue;
  }

  public ButtonAction setAdditionalDialogue(boolean additionalDialogue) {
    this.additionalDialogue = additionalDialogue;
    return this;
  }

  public String getMessageDate() {
    return messageDate;
  }

  public ButtonAction setMessageDate(String messageDate) {
    this.messageDate = messageDate;
    return this;
  }

  public String getReplyButton() {
    return replyButton;
  }

  public ButtonAction setReplyButton(String replyButton) {
    this.replyButton = replyButton;
    return this;
  }

  public String getCompositeMessage() {
    return compositeMessage;
  }

  public ButtonAction setCompositeMessage(String compositeMessage) {
    this.compositeMessage = compositeMessage;
    return this;
  }

  @Override
  public String toString() {
    return "ButtonAction{" +
        "button=" + button +
        ", additionalDialogue=" + additionalDialogue +
        ", messageDate='" + messageDate + '\'' +
        ", replyButton='" + replyButton + '\'' +
        ", compositeMessage='" + compositeMessage + '\'' +
        '}';
  }
}
