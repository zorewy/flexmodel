package tech.wetech.flexmodel.validations;

import tech.wetech.flexmodel.TypedField;

import java.time.LocalDate;

/**
 * @author cjbi
 */
public class DateMaxValidator extends AbstractConstraintValidator<LocalDate> {

  private final LocalDate max;

  public DateMaxValidator(LocalDate max) {
    this("must be greater than or equal to {{min}}", max);
  }

  public DateMaxValidator(String message, LocalDate max) {
    super(message);
    this.max = max;
  }

  @Override
  public void validate(TypedField<LocalDate, ?> field, LocalDate value) throws ConstraintValidException {
    if (value == null || value.isAfter(max)) {
      handleThrows(field, value);
    }
  }

  public LocalDate getMax() {
    return max;
  }
}
