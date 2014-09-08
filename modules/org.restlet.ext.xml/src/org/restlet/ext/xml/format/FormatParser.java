package org.restlet.ext.xml.format;

import java.io.Reader;
import java.util.List;

/**
 * Deals with parsing the resulting stream into respective entity object.
 * The implementation depends on the format Atom or Json.
 *
 * @param <T> Feed
 * 
 * @author <a href="mailto:onkar.dhuri@synerzip.com">Onkar Dhuri</a>
 *
 */
public interface FormatParser<T> {

  /**
   * Parses the feed from reader
   *
   * @param reader the reader
   * @return the t
   */
  void parse(Reader reader);
  
  /**
   * Returns the list of entities parsed/populated by the parse() method
   *
   * @return the entities
   */
  List<T> getEntities();
  
  /**
   * Returns the feed associated with the parser.
   *
   * @param <Feed> the generic type
   * @return the feed
   */
  <Feed> Feed getFeed();

}
