package tech.wetech.flexmodel;

/**
 * @author cjbi
 */
public class StringTypeHandler implements TypeHandler<String> {
  @Override
  public String convertParameter(Object value) {
    if (value == null) {
      return null;
    }
    return value.toString();
  }
}
