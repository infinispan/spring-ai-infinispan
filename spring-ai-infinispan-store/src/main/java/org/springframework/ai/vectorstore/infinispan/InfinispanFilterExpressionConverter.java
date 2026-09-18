/*
 * Copyright 2023-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ai.vectorstore.infinispan;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.converter.AbstractFilterExpressionConverter;
import org.springframework.util.Assert;

class InfinispanFilterExpressionConverter extends AbstractFilterExpressionConverter {

	private static final Pattern DATE_FORMAT_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
		.withZone(ZoneOffset.UTC);

	private int i = -1;

	public String doJoin() {
		StringBuilder sb = new StringBuilder();
		for (int j = 0; j <= this.i; j++) {
			sb.append(" join i.metadata m").append(j);
		}
		return sb.toString();
	}

	@Override
	protected void doExpression(Filter.Expression expression, StringBuilder context) {
		switch (expression.type()) {
			case AND:
				context.append("((");
				doExpression(convertToFilterExpression(expression.left()), context);
				context.append(") AND (");
				doExpression(convertToFilterExpression(expression.right()), context);
				context.append("))");
				break;
			case OR:
				context.append("((");
				doExpression(convertToFilterExpression(expression.left()), context);
				context.append(") OR (");
				doExpression(convertToFilterExpression(expression.right()), context);
				context.append("))");
				break;
			default:
				doField(expression, context);
				break;
		}
	}

	@Override
	protected void doSingleValue(Object value, StringBuilder context) {
		if (value instanceof Date date) {
			context.append(date.toInstant().toEpochMilli());
		}
		else if (value instanceof Integer intVal) {
			context.append(intVal.longValue());
		}
		else if (value instanceof Long longVal) {
			context.append(longVal);
		}
		else if (value instanceof Float floatVal) {
			context.append(floatVal.doubleValue());
		}
		else if (value instanceof Double doubleVal) {
			context.append(doubleVal);
		}
		else if (value instanceof Boolean bool) {
			context.append(bool.booleanValue());
		}
		else {
			context.append(escape(String.valueOf(value)));
		}
	}

	@Override
	protected void doKey(Filter.Key filterKey, StringBuilder context) {

	}

	@Override
	public void doStartGroup(Filter.Group group, StringBuilder context) {
		context.append("(");
	}

	@Override
	public void doEndGroup(Filter.Group group, StringBuilder context) {
		context.append(")");
	}

	private static Filter.Expression convertToFilterExpression(Filter.@Nullable Operand expression) {
		if (expression instanceof Filter.Expression) {
			return (Filter.Expression) expression;
		}

		throw new IllegalArgumentException("Expected a Filter expression");
	}

	private void doField(Filter.Expression expression, StringBuilder context) {
		Filter.Key key = (Filter.Key) expression.left();
		Filter.Value value = (Filter.Value) expression.right();
		String result = switch (expression.type()) {
			case EQ -> mapEqual(key, value, true);
			case NE -> mapEqual(key, value, false);
			case GT -> mapGreaterThan(key, value);
			case GTE -> mapGreaterThanEqual(key, value);
			case LT -> mapLessThan(key, value);
			case LTE -> mapLessThanEqual(key, value);
			case IN -> mapIn(key, value, true);
			case NIN -> mapIn(key, value, false);
			case ISNULL -> mapIsNull(key);
			case ISNOTNULL -> mapIsNotNull(key);
			default -> throw new UnsupportedOperationException("Unsupported value: " + expression.type());
		};
		context.append(result);
	}

	private String mapIsNull(Filter.Key key) {
		incrementJoin();
		String m = "m" + this.i + ".";
		return "(" + metadataKey(key) + String.format(
				"%svalue IS NULL and %svalue_int IS NULL and %svalue_date IS NULL and %svalue_float IS NULL and %svalue_bool IS NULL)",
				m, m, m, m, m) + " OR (" + m + "name" + " NOT IN(" + escape(key.key()) + "))";
	}

	private String mapIsNotNull(Filter.Key key) {
		incrementJoin();
		String m = "m" + this.i + ".";
		return metadataKey(key) + String.format(
				"(%svalue IS NOT NULL or %svalue_int IS NOT NULL or %svalue_date IS NOT NULL or %svalue_float IS NOT NULL or %svalue_bool IS NOT NULL)",
				m, m, m, m, m);
	}

	private String mapEqual(Filter.Key key, Filter.@Nullable Value value, boolean equals) {
		Assert.notNull(value, "value must not be null");
		incrementJoin();
		String filter = metadataKey(key) + computeValue(equals ? "=" : "!=", value.value());
		if (equals) {
			return filter;
		}
		return filter + " " + addMetadataNullCheck();
	}

	private String mapGreaterThan(Filter.Key key, Filter.@Nullable Value value) {
		Assert.notNull(value, "value must not be null");
		incrementJoin();
		return metadataKey(key) + computeValue(">", value.value());
	}

	private String mapGreaterThanEqual(Filter.Key key, Filter.@Nullable Value value) {
		Assert.notNull(value, "value must not be null");
		incrementJoin();
		return metadataKey(key) + computeValue(">=", value.value());
	}

	private String mapLessThan(Filter.Key key, Filter.@Nullable Value value) {
		Assert.notNull(value, "value must not be null");
		incrementJoin();
		return metadataKey(key) + computeValue("<", value.value());
	}

	private String mapLessThanEqual(Filter.Key key, Filter.@Nullable Value value) {
		Assert.notNull(value, "value must not be null");
		incrementJoin();
		return metadataKey(key) + computeValue("<=", value.value());
	}

	private String mapIn(Filter.Key key, Filter.@Nullable Value value, boolean in) {
		Assert.notNull(value, "value must not be null");
		incrementJoin();
		String inStatement;
		List<?> values;
		if (value.value() instanceof List<?> list) {
			if (list.isEmpty()) {
				throw new UnsupportedOperationException("Infinispan metadata filter IN must contain values");
			}
			values = list;
		}
		else {
			values = List.of(value.value());
		}

		validateNoMixedTypes(values);
		String column = valueColumn(values);
		inStatement = formattedComparisonValues(values, column);

		String m = "m" + this.i + ".";
		String inFilter = m + column + " IN (" + inStatement + ")";

		if (in) {
			return metadataKey(key) + inFilter;
		}

		String notInFilter = m + column + " NOT IN (" + inStatement + ")";

		return "(" + notInFilter + metadataKeyLast(key) + ") " + "OR (" + inFilter + " and " + m + "name!="
				+ escape(key.key()) + ")" + " " + addMetadataNullCheck();
	}

	private String metadataKey(Filter.Key key) {
		return "m" + this.i + ".name=" + escape(key.key()) + " and ";
	}

	private String metadataKeyLast(Filter.Key key) {
		return " and m" + this.i + ".name=" + escape(key.key()) + " ";
	}

	private String computeValue(String operator, Object value) {
		String m = "m" + this.i + ".";
		String filterQuery = "";
		if (value instanceof String text && DATE_FORMAT_PATTERN.matcher(text).matches()) {
			try {
				filterQuery = m + "value_date" + operator
						+ Instant.from(DATE_TIME_FORMATTER.parse(text)).toEpochMilli();
			}
			catch (DateTimeParseException e) {
				throw new IllegalArgumentException("Invalid date type:" + text, e);
			}
		}
		else if (value instanceof Integer || value instanceof Long) {
			Long longValue = getLongValue(value);
			filterQuery = m + "value_int" + operator + longValue;
		}
		else if (value instanceof Float || value instanceof Double) {
			Double doubleValue = getDoubleValue(value);
			filterQuery = m + "value_float" + operator + doubleValue;
		}
		else if (value instanceof Date || value instanceof Instant) {
			filterQuery = m + "value_date" + operator + getDate(value);
		}
		else if (value instanceof Boolean bool) {
			filterQuery = m + "value_bool" + operator + bool.booleanValue();
		}
		else {
			filterQuery = m + "value" + operator + escape(String.valueOf(value));
		}
		return filterQuery;
	}

	private long getDate(Object value) {
		if (value instanceof Date date) {
			return date.toInstant().toEpochMilli();
		}
		if (value instanceof Instant instant) {
			return instant.toEpochMilli();
		}
		return 0L;
	}

	private Long getLongValue(Object value) {
		return value instanceof Integer ? ((Integer) value).longValue() : (Long) value;
	}

	private Double getDoubleValue(Object value) {
		return value instanceof Float ? ((Float) value).doubleValue() : (Double) value;
	}

	private String valueColumn(Collection<?> values) {
		if (values.stream().anyMatch(v -> v instanceof Float || v instanceof Double)) {
			return "value_float";
		}
		if (values.stream().anyMatch(v -> v instanceof Integer || v instanceof Long)) {
			return "value_int";
		}
		if (values.stream().anyMatch(v -> v instanceof Boolean)) {
			return "value_bool";
		}
		if (values.stream().anyMatch(v -> v instanceof Date)) {
			return "value_date";
		}
		return "value";
	}

	private void validateNoMixedTypes(Collection<?> values) {
		boolean hasNumeric = values.stream().anyMatch(v -> v instanceof Number);
		boolean hasNonNumeric = values.stream().anyMatch(v -> !(v instanceof Number));
		if (hasNumeric && hasNonNumeric) {
			throw new IllegalArgumentException(
					"Infinispan metadata filter IN/NOT IN cannot mix numeric and non-numeric values");
		}
	}

	private String formattedComparisonValues(Collection<?> comparisonValues, String column) {
		boolean asFloat = "value_float".equals(column);
		return comparisonValues.stream().map(s -> formatComparisonValue(s, asFloat)).collect(Collectors.joining(", "));
	}

	private String formatComparisonValue(Object value, boolean asFloat) {
		if (value instanceof Date date) {
			return "'" + date + "'";
		}
		if (!(value instanceof Number)) {
			return escape(String.valueOf(value));
		}
		if (asFloat) {
			return String.valueOf(((Number) value).doubleValue());
		}
		if (value instanceof Integer) {
			return String.valueOf(((Integer) value).longValue());
		}
		return value.toString();
	}

	private String addMetadataNullCheck() {
		return "OR (i.metadata is null)";
	}

	private void incrementJoin() {
		this.i++;
	}

	private static String escape(String s) {
		return "'" + s.replace("\\", "\\\\").replace("'", "''") + "'";
	}

}
