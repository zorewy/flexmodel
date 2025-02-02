package tech.wetech.flexmodel;

import java.time.LocalDate;

/**
 * @author cjbi
 */
public class DateField extends TypedField<LocalDate, DateField> {

  public DateField(String name) {
    super(name, BasicFieldType.DATE.getType());
  }

}
