package com.dont.groovy.utils.meta.database

import com.dieselpoint.norm.Util
import com.dieselpoint.norm.sqlmakers.MySqlMaker
import com.dieselpoint.norm.sqlmakers.Property
import com.dieselpoint.norm.sqlmakers.StandardPojoInfo
import com.dont.groovy.utils.meta.database.converters.CollectionAdapter
import com.dont.groovy.utils.meta.database.converters.ItemAdapter
import com.dont.groovy.utils.meta.database.converters.LocationConverter
import org.bukkit.Location
import org.bukkit.inventory.ItemStack

import javax.persistence.Column
import javax.persistence.Id
import java.util.concurrent.ConcurrentHashMap

class CustomMysqlMaker extends MySqlMaker {

    private static ConcurrentHashMap<Class<?>, StandardPojoInfo> pogos = new ConcurrentHashMap<Class<?>, StandardPojoInfo>();

    @Override
    StandardPojoInfo getPojoInfo(Class<?> rowClass) {
        StandardPojoInfo pi = pogos.get(rowClass);
        if (pi == null) {
            pi = new StandardPojoInfo(rowClass);
            pi.propertyMap.remove("metaClass");
            rowClass.getDeclaredFields().findAll({ it.isAnnotationPresent(Id) }).each {
                pi.propertyMap.get(it.name).isPrimaryKey = true
                pi.primaryKeyName = it.name
            }
            pi.propertyMap.values().each {
                if (it.converter != null) return
                if (it.dataType == Location) it.converter = LocationConverter.INSTANCE
                else if (it.dataType == ItemStack) it.converter = ItemAdapter.INSTANCE
                else if (Map.isAssignableFrom(it.dataType) || Collection.isAssignableFrom(it.dataType)) it.converter = new CollectionAdapter(rowClass.getDeclaredField(it.name))
            }
            pogos.put(rowClass, pi);
            makeInsertSql(pi);
            makeUpsertSql(pi);
            makeUpdateSql(pi);
            makeSelectColumns(pi);
        }
        return pi;
    }

    @Override
    private void makeSelectColumns(StandardPojoInfo pojoInfo) {
        if (pojoInfo.propertyMap.isEmpty()) {
            // this applies if the rowClass is a Map
            pojoInfo.selectColumns = "*";
        } else {
            ArrayList<String> cols = new ArrayList<String>();
            for (Property prop : pojoInfo.propertyMap.values()) {
                cols.add(prop.name);
            }
            pojoInfo.selectColumns = Util.join(cols);
        }
    }

    @Override
    protected String getColType(Class<?> dataType, int length, int precision, int scale) {
        String colType;

        if (dataType.equals(Integer.class) || dataType.equals(int.class)) {
            colType = "integer";

        } else if (dataType.equals(Long.class) || dataType.equals(long.class)) {
            colType = "bigint";

        } else if (dataType.equals(Double.class) || dataType.equals(double.class)) {
            colType = "double";

        } else if (dataType.equals(Float.class) || dataType.equals(float.class)) {
            colType = "float";

        } else if (dataType.equals(BigDecimal.class)) {
            colType = "decimal(" + precision + "," + scale + ")";

        } else if (dataType.equals(java.util.Date.class)) {
            colType = "datetime";

        } else {
            colType = "mediumtext";
        }
        return colType;
    }

    @Override
    String getCreateTableSql(Class<?> clazz) {

        StringBuilder buf = new StringBuilder();

        StandardPojoInfo pojoInfo = getPojoInfo(clazz);
        buf.append("create table ");
        buf.append(pojoInfo.table);
        buf.append(" (");

        boolean needsComma = false;
        for (Property prop : pojoInfo.propertyMap.values()) {

            if (needsComma) {
                buf.append(',');
            }
            needsComma = true;

            Column columnAnnot = prop.columnAnnotation;
            if (columnAnnot == null) {

                buf.append(prop.name);
                buf.append(" ");
                buf.append(getColType(prop.dataType, 255, 10, 2));
                if (prop.isGenerated) {
                    buf.append(" auto_increment");
                }

            } else {
                if (columnAnnot.columnDefinition() == null) {

                    // let the column def override everything
                    buf.append(columnAnnot.columnDefinition());

                } else {

                    buf.append(prop.name);
                    buf.append(" ");
                    buf.append(getColType(prop.dataType, columnAnnot.length(), columnAnnot.precision(), columnAnnot.scale()));
                    if (prop.isGenerated) {
                        buf.append(" auto_increment");
                    }

                    if (columnAnnot.unique()) {
                        buf.append(" unique");
                    }

                    if (!columnAnnot.nullable()) {
                        buf.append(" not null");
                    }
                }
            }
        }

        if (pojoInfo.primaryKeyName != null) {
            buf.append(", primary key (");
            buf.append(pojoInfo.primaryKeyName);
            buf.append("(64))");
        }

        buf.append(")");

        return buf.toString();
    }

}
