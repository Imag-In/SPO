package org.icroco.picture.persistence.model;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

public class PathType implements UserType<Path> {
    @Override
    public int getSqlType() {
        return Types.VARCHAR;
    }

    @Override
    public Class<Path> returnedClass() {
        return Path.class;
    }

    @Override
    public boolean equals(Path o1, Path o2) {
        return Objects.equals(o1, o2);
    }

    @Override
    public int hashCode(Path o) {
        return Objects.hashCode(o);
    }


    @Override
    public Path nullSafeGet(ResultSet rs, int i, SharedSessionContractImplementor sharedSessionContractImplementor, Object o) throws SQLException {
        if (rs.wasNull())
            return null;

        return Paths.get(rs.getString(i));
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Path value, int i, SharedSessionContractImplementor sharedSessionContractImplementor)
            throws SQLException {
        st.setString(i, value == null ? null : value.toString());
    }

    @Override
    public Path deepCopy(Path value) {
        return value == null ? null : Paths.get(value.toString());
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Path value) {
        return (Serializable) value;
    }

    @Override
    public Path assemble(Serializable serializable, Object o) {
        return (Path) serializable;
    }

    @Override
    public Path replace(Path original, Path target, Object owner) {
        return original;
    }
}
