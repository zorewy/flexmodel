package tech.wetech.flexmodel;

/**
 * @author cjbi
 */
public class DecimalTypeHandler implements TypeHandler<Double> {
  @Override
  public Double convertParameter(Object value) {
    if (value instanceof Number number) {
      return number.doubleValue();
    }
    return null;
  }
}
