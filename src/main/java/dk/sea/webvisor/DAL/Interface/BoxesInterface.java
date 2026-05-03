package dk.sea.webvisor.DAL.Interface;

import dk.sea.webvisor.BE.Boxes;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface BoxesInterface
{
    Optional<Boxes> getBoxById(String boxId) throws SQLException;
    List<Boxes> getAllBoxes() throws SQLException;
    Boxes createBox(String boxId) throws SQLException;
    void deleteBox(String boxId) throws SQLException;
}
