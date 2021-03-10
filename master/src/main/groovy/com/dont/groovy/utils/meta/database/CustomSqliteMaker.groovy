package com.dont.groovy.utils.meta.database

import com.dieselpoint.norm.Query
import com.dieselpoint.norm.Util
import com.dieselpoint.norm.sqlmakers.Property
import com.dieselpoint.norm.sqlmakers.StandardPojoInfo

import javax.persistence.Column

class CustomSqliteMaker extends CustomMysqlMaker {

    @Override
    void makeInsertSql(StandardPojoInfo pojoInfo) {
        ArrayList<String> cols = new ArrayList<String>();
        for (Property prop : pojoInfo.propertyMap.values()) {
            if (prop.isGenerated) {
                continue;
            }
            cols.add(prop.name);
        }
        pojoInfo.insertColumnNames = cols.toArray(new String[cols.size()]);
        pojoInfo.insertSqlArgCount = pojoInfo.insertColumnNames.length;

        StringBuilder buf = new StringBuilder();
        buf.append("insert or replace into ");
        buf.append(pojoInfo.table);
        buf.append(" (");
        buf.append(Util.join(pojoInfo.insertColumnNames)); // comma sep list?
        buf.append(") values (");
        buf.append(Util.getQuestionMarks(pojoInfo.insertSqlArgCount));
        buf.append(")");

        pojoInfo.insertSql = buf.toString();
    }

    @Override
    void makeUpsertSql(StandardPojoInfo pojoInfo) {
        pojoInfo.upsertSql = pojoInfo.insertSql
    }

    Object[] getUpsertArgs(Query query, Object row) {
        return super.getInsertArgs(query, row);
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
            buf.append(")");
        }

        buf.append(")");

        return buf.toString();
    }

}
