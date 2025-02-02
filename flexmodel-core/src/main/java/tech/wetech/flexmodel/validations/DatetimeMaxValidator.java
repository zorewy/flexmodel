package tech.wetech.flexmodel.validations;

import tech.wetech.flexmodel.TypedField;

import java.time.LocalDateTime;

/**
 * @author cjbi
 */
public class DatetimeMaxValidator extends AbstractConstraintValidator<LocalDateTime> {

  private final LocalDateTime max;

  public DatetimeMaxValidator(LocalDateTime max) {
    this("must be greater than or equal to {{min}}", max);
  }

  public DatetimeMaxValidator(String message, LocalDateTime max) {
    super(message);
    this.max = max;
  }

  @Override
  public void validate(TypedField<LocalDateTime, ?> field, LocalDateTime value) throws ConstraintValidException {
    if (value == null || value.isAfter(max)) {
      handleThrows(field, value);
    }
  }

  public LocalDateTime getMax() {
    return max;
  }
}
