package com.mnsuk.castudio.numbers;

import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Stack;

import com.ibm.talent.jfst.norm.AbstractStringNormalizer;
import com.ibm.talent.jfst.norm.JFSTNormalizerException;

public class Numbers extends AbstractStringNormalizer {

	@Override
	public String normalize(String arg0, String[] arg1) throws JFSTNormalizerException {
		int decimalFraction;
		if (arg1.length == 2) {
			try {
				decimalFraction = Integer.parseInt(arg1[1]);
			} catch (NumberFormatException e) {
				decimalFraction = 2;
			}
		} else {
			decimalFraction = 2;
		}
		return numberNormalise(arg1[0], decimalFraction);
	}

	/** 
	 * Given a number annotation (text or digits) updates the feature holding the
	 * number as a decimal string in a specified normalised form.
	 * <p>
	 * 
	 * 02/08/11 
	 * Input format strings supported:
	 * 			a) numerics e.g. 12,345,678.00
	 * 			b) cardinals e.g. one million four hundred and thirty two thousand
	 * 			c) numerics with magnitude e.g. 16.2B
	 * 			d) numeric multiplier with single cardinal e.g. 72 million
	 * 			e) integer numeric and cardinals e.g. 8 hundred and fifty thousand
	 * 21/02/14
	 * 			f) fractions e.g. 1/3
	 * 			g) fractions with whole numbers  2 3/4
	 * 			to do
	 * 			h) fractions e.g. one-fourth, three and a half
	 * 
	 * Output feature is decimal string with no grouping separators and configurable 
	 * fractional precision e.g. 1234567.89
	 * <p>
	 * @param  num  Number string to be normalised. 
	 * @param precision Precision to use on output decimal.
	 */
	private String numberNormalise(String num, int precision) {
		boolean fail=false;

		String ct = num;
		NumberFormat numberformat = NumberFormat.getInstance();
		java.lang.Number value = null;
		ParsePosition pp = new ParsePosition(0);
		value = numberformat.parse(ct, pp);
		Integer numerator = null, denominator = null;
		boolean fraction = false;
		if( ct.length() != pp.getIndex()) {
			// the whole string wasn't parsed therefore it's not type a (above)
			String parseString;
			if (ct.length() == pp.getIndex()+1) {  // type c
				char aChar = ct.charAt(pp.getIndex());
				switch (aChar) {
				case 'k':
				case 'K':
					value = value.doubleValue() * 1000d;
					break;
				case 'm':
				case 'M':
					value = value.doubleValue() * 1000000d;
					break;
				case 'b':
				case 'B':
					value = value.doubleValue() * 1000000000d;
					break;
				case 't':
				case 'T':
					value = value.doubleValue() * 1000000000000d;								
					break;
				default:
					// parse error
					fail = true;
					break;							
				}
			} else {
				if ( value != null) {
					String str = ct.substring(pp.getIndex()).trim();
					if (str.contains("/")) { // type f or g
						fraction = true;
						try {
							if (str.startsWith("/")) { // type f
								numerator = value.intValue();
								denominator = Integer.parseInt(str.substring(1));
								value = value.doubleValue() / Double.parseDouble(str.substring(1));
							} else { // type g

								parseString = ct.substring(pp.getIndex()).trim();
								String[] frac = parseString.split("/");
								denominator = Integer.parseInt(frac[1]);
								value = value.doubleValue() + Double.parseDouble(frac[0]) / Double.parseDouble(frac[1]);
								numerator = value.intValue() * denominator + Integer.parseInt(frac[0]);	
							}
						} catch (Exception e) {
							fail = true;
							//logger.log(Level.INFO, "Parsing type f or g of the annotation Number = {" + ct + "}\n" + e.toString(), e);
						}
					} else if (str.indexOf(' ') == -1) { // type d
						try {
							parseString = ct.substring(pp.getIndex()).trim();
							value = value.doubleValue() * cardinalsToNumber(parseString);
						} catch (Exception e) {
							fail = true;
							//logger.log(Level.INFO, "Parsing type d of the annotation Number = {" + ct + "}\n" + e.toString(), e);
						}
					} else { // type e
						try {
							parseString = numberToCardinals(value) + " " + ct.substring(pp.getIndex()).trim();
							value = cardinalsToNumber(parseString);
						} catch (Exception e) {
							fail = true;
							//logger.log(Level.INFO, "Parsing type e of the annotation Number = {" + ct + "}\n" + e.toString(), e);
						}
					}
				} else {
					// type b or h
					parseString = ct.substring(pp.getIndex()).trim();
					parseString = parseString.toLowerCase().replaceAll("[\\-,]", " ").trim();
					String[] words = parseString.split("\\s");
					for (String word : words) {
						if (denominators.containsKey(word)) { // type h
							fraction = true;
							denominator = denominators.get(word);
							int index = parseString.indexOf("and");
							if (index >= 0) {
								String text1 = parseString.substring(0, index).trim();
								String text2 = parseString.substring(index + "and".length()).trim();
								index = text2.indexOf(word);
								String text3 = text2.substring(0, index);
								try {
									Number wholeNumb = cardinalsToNumber(text1);
									Number fractionNumerator = cardinalsToNumber(text3);
									numerator = wholeNumb.intValue() * denominator + fractionNumerator.intValue();
									value = numerator.doubleValue() / denominator.doubleValue();
								} catch (Exception e) {
									fail = true;
									//logger.log(Level.INFO, "Parsing type h of the annotation Number = {" + ct + "}\n" + e.toString(), e);
								}
							} else {
								index = parseString.indexOf(word);
								String text1 = parseString.substring(0, index);
								try {
									Number fractionNumerator = cardinalsToNumber(text1);
									numerator = fractionNumerator.intValue();
									value = numerator.doubleValue() / denominator.doubleValue();
								} catch (Exception e) {
									fail = true;
									//logger.log(Level.INFO, "Parsing type h of the annotation Number = {" + ct + "}\n" + e.toString(), e);
								}
							}
							break;
						}
					}
					if (!fraction) { //type b
						try {
							value = cardinalsToNumber(parseString);
						} catch (Exception e) {
							fail = true;
							//logger.log(Level.INFO, "Parsing type b of the annotation Number = {" + ct + "}\n" + e.toString(), e);
						}
					}
				}
			}
		}


		if (fail) {
			return "NaN";
		} else {
			double dvalue = value.doubleValue();
			numberformat.setMaximumFractionDigits(precision);
			numberformat.setMinimumFractionDigits(precision);
			numberformat.setGroupingUsed(false);
			return numberformat.format(dvalue);	
		}
	}

	private static final String[] magWords = { "trillion", "billion", "million", "thousand" };
	private static final long[] magDigits = { 1000000000000L, 1000000000L,1000000L, 1000L };
	// hundreds, thousands, hundreds of thousands, millions...
	private static final String cardinalMagnitude[] = { "blank", "hundred", "thousand", "hundred", "million" };

	private static final HashMap<String, Integer> ordinalNumeric;
	private static final HashMap<Integer, String> numericOrdinal;
	private static final HashMap<String, Integer> denominators;


	static {
		ordinalNumeric = new HashMap<String, Integer>();
		ordinalNumeric.put("zero", 0);
		ordinalNumeric.put("one", 1);
		ordinalNumeric.put("two", 2);
		ordinalNumeric.put("three", 3);
		ordinalNumeric.put("four", 4);
		ordinalNumeric.put("five", 5);
		ordinalNumeric.put("six", 6);
		ordinalNumeric.put("seven", 7);
		ordinalNumeric.put("eight", 8);
		ordinalNumeric.put("nine", 9);
		ordinalNumeric.put("ten", 10);
		ordinalNumeric.put("eleven", 11);
		ordinalNumeric.put("twelve", 12);
		ordinalNumeric.put("thirteen", 13);
		ordinalNumeric.put("fourteen", 14);
		ordinalNumeric.put("fifteen", 15);
		ordinalNumeric.put("sixteen", 16);
		ordinalNumeric.put("seventeen", 17);
		ordinalNumeric.put("eighteen", 18);
		ordinalNumeric.put("nineteen", 19);
		ordinalNumeric.put("twenty", 20);
		ordinalNumeric.put("thirty", 30);
		ordinalNumeric.put("forty", 40);
		ordinalNumeric.put("fifty", 50);
		ordinalNumeric.put("sixty", 60);
		ordinalNumeric.put("seventy", 70);
		ordinalNumeric.put("eighty", 80);
		ordinalNumeric.put("ninety", 90);
		ordinalNumeric.put("hundred", 100);

		numericOrdinal = new HashMap<Integer, String>();
		numericOrdinal.put(0, "zero");
		numericOrdinal.put(1, "one");
		numericOrdinal.put(2, "two");
		numericOrdinal.put(3, "three");
		numericOrdinal.put(4, "four");
		numericOrdinal.put(5, "five");
		numericOrdinal.put(6, "six");
		numericOrdinal.put(7, "seven");
		numericOrdinal.put(8, "eight");
		numericOrdinal.put(9, "nine");
		numericOrdinal.put(10, "ten");
		numericOrdinal.put(11, "eleven");
		numericOrdinal.put(12, "twelve");
		numericOrdinal.put(13, "thirteen");
		numericOrdinal.put(14, "fourteen");
		numericOrdinal.put(15, "fifteen");
		numericOrdinal.put(16, "sixteen");
		numericOrdinal.put(17, "seventeen");
		numericOrdinal.put(18, "eighteen");
		numericOrdinal.put(19, "nineteen");
		numericOrdinal.put(20, "twenty");
		numericOrdinal.put(30, "thirty");
		numericOrdinal.put(40, "forty");
		numericOrdinal.put(50, "fifty");
		numericOrdinal.put(60, "sixty");
		numericOrdinal.put(70, "seventy");
		numericOrdinal.put(80, "eighty");
		numericOrdinal.put(90, "ninety");
		numericOrdinal.put(100, "hundred");

		denominators = new HashMap<String, Integer>();
		denominators.put("half",2);
		denominators.put("third",3);
		denominators.put("thirds",3);
		denominators.put("fourth",4);
		denominators.put("fourths",4);
		denominators.put("quarter",4);
		denominators.put("quarters",4);
		denominators.put("fifth",5);
		denominators.put("fifths",5);
		denominators.put("eigth",8);
		denominators.put("eigths",8);

	}

	private Long cardinalsToNumber(String text) throws Exception {
		// text = text.toLowerCase().replaceAll("[\\-,a]", " ").replaceAll(" and "," ").trim();
		text = text.toLowerCase().replaceAll("a ", "one ");
		text = text.toLowerCase().replaceAll("[\\-,]", " ").replaceAll(" and "," ").trim();
		long totalValue = 0;
		boolean processed = false;
		for (int n = 0; n < magWords.length; n++) {
			int index = text.indexOf(magWords[n]);
			if (index >= 0) {
				String text1 = text.substring(0, index).trim();
				String text2 = text.substring(index + magWords[n].length()).trim();

				if (text1.equals(""))
					text1 = "one";

				if (text2.equals("") || text2.equals("s"))  // final word or final word with "s" on end. 
					text2 = "zero";

				if (text2.startsWith("s ")) // previous word word is pluralised e.g millions
					text2 = text2.substring(2);

				totalValue = parseNumerals(text1) * magDigits[n]+ cardinalsToNumber(text2);
				processed = true;
				break;
			}
		}

		if (processed)
			return totalValue;
		else
			return parseNumerals(text);
	}

	private long parseNumerals(String text) throws Exception {
		long value = 0;
		String[] words = text.replaceAll(" and ", " ").split("\\s");
		for (String word : words) {
			if (!ordinalNumeric.containsKey(word)) {
				throw new Exception("Unknown token : " + word);
			}
			long subval = ordinalNumeric.get(word);
			if (subval == 100) {
				if (value == 0)
					value = 100;
				else
					value *= 100;
			} else
				value += subval;       	
		}

		return value;
	}

	private String numberToCardinals(Number inumber)  throws Exception {
		int inputNumber = inumber.intValue();
		int n = 0;
		int digitsToProcess;

		Stack<String>stack = new Stack<String>(); 

		while (inputNumber != 0){
			switch (n) {
			case 0: // 1 to 99,  the first two digits
				digitsToProcess = inputNumber % 100;
				stack.push(parseDigits(digitsToProcess));
				if (inputNumber > 100 && inputNumber % 100 != 0)
					stack.push("and ");   // n hundred AND nn
				inputNumber /= 100;  // trunc digits
				break;
			case 1: // 1 hundred to 9 hundred, the single digits
				digitsToProcess = inputNumber % 10;
				if (digitsToProcess != 0) {
					stack.push(" ");
					stack.push(cardinalMagnitude[n]);
					stack.push(" ");
					stack.push(parseDigits(digitsToProcess));
				}
				inputNumber /= 10;  // trunc digits
				break;	
			case 2: // 1 thousand to 99 thousand, 2 digits
				digitsToProcess = inputNumber % 100;
				if (digitsToProcess != 0) {
					stack.push(" ");
					stack.push(cardinalMagnitude[n]);
					stack.push(" ");
					stack.push(parseDigits(digitsToProcess));
				}
				if (inputNumber > 100 && inputNumber % 100 != 0)
					stack.push("and ");   // n hundred AND nn thousand  
				else if (inputNumber > 99)
					stack.push("thousand "); // n hundred THOUSAND  (no other thousands)
				inputNumber /= 100;
				break;                
				// 1--,--- to 9--,---
			case 3: // 1 hundred thousand to 9 hundred thousand, single digit
				digitsToProcess = inputNumber % 10;
				if (digitsToProcess != 0) {
					stack.push(" ");
					stack.push(cardinalMagnitude[n]);
					stack.push(" ");
					stack.push(parseDigits(digitsToProcess));
				}
				inputNumber /= 10;  // trunc digits
				break;
			case 4: // 1 million to 99 million, 2 digits
				digitsToProcess = inputNumber % 100;
				if (digitsToProcess != 0) {
					stack.push(" ");
					stack.push(cardinalMagnitude[n]);
					stack.push(" ");
					stack.push(parseDigits(digitsToProcess));
				}
				inputNumber /= 100;  // trunc digits
				break;
			default:
				throw new Exception("Resultant cardinal number too large.");
			}
			n++;
		}

		String str= new String();
		while (!stack.empty ()) { 
			str += stack.pop (); 
		} 
		return str;

	}



	private String parseDigits(int number)  throws Exception  {
		String retStr = new String();
		String strUnits = new String();
		String strTens = new String();
		int units, tens;

		if (number < 19 || number % 10 == 0) {       	
			if (!numericOrdinal.containsKey(number)) {
				throw new Exception("Unknown number : " + number);
			}
			retStr = numericOrdinal.get(number);                	
		} else {
			units = number % 10;
			tens = number - units;
			if (!numericOrdinal.containsKey(tens)) {
				throw new Exception("Unknown number : " + tens);
			}
			strTens = numericOrdinal.get(tens);
			if (!numericOrdinal.containsKey(units)) {
				throw new Exception("Unknown number : " + units);
			}
			strUnits = numericOrdinal.get(units);
			retStr = strTens + " " + strUnits;        	
		}
		return retStr;
	}
}
