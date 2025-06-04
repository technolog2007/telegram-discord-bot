package pkpm.telegrambot.models;

public class ButtonAction {

  private Buttons button;
  private boolean additionalDialogue = false;
  private String messageDate;

  public ButtonAction(Buttons button){
    this.button = button;
  }

  public ButtonAction(Buttons button, boolean additionalDialogue, String messageDate) {
    this.button = button;
    this.additionalDialogue = additionalDialogue;
    this.messageDate = messageDate;
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

  @Override
  public String toString() {
    return "ButtonAction{" +
        "button=" + button +
        ", additionalDialogue=" + additionalDialogue +
        ", messageDate='" + messageDate + '\'' +
        '}';
  }
}
