public class RegisterAllocator
{
  public  String regValue;
  public  boolean regStatus;
  
  public RegisterAllocator(String value, boolean isDirty)
  {
    this.regValue = value;
    this.regStatus = isDirty;
  }

  public  void setRegValue(String value)
  {
    regValue = value;
  }

  public  void setRegStatus(boolean isDirty)
  {
    regStatus = isDirty;
  }

  public  String getRegValue()
  {
    return regValue; 
  }

  public boolean getRegStatus()
  {
    return regStatus;
  }
}
