package tech.wetech.flexmodel.validations;

import tech.wetech.flexmodel.TypedField;

import java.time.LocalDate;

/**
 * @author cjbi
 */
public class DateRangeValidator extends AbstractConstraintValidator<LocalDate> {

  private final LocalDate min;
  private final LocalDate max;

  public DateRangeValidator(LocalDate min, LocalDate max) {
    this("must be between {{min}} and {{max}}", min, max);
  }

  public DateRangeValidator(String message, LocalDate min, LocalDate max) {
    super(message);
    this.min = min;
    this.max = max;
  }

  @Override
  public void validate(TypedField<LocalDate, ?> field, LocalDate value) throws ConstraintValidException {
    if (value == null || (value.isBefore(min) || value.isAfter(max))) {
      handleThrows(field, value);
    }
  }

  public LocalDate getMin() {
    return min;
  }

  public LocalDate getMax() {
    return max;
  }
}
