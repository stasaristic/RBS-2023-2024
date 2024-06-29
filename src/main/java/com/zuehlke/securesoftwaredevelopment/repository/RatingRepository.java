package com.zuehlke.securesoftwaredevelopment.repository;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.config.Entity;
import com.zuehlke.securesoftwaredevelopment.domain.Comment;
import com.zuehlke.securesoftwaredevelopment.domain.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RatingRepository {

    private static final Logger LOG = LoggerFactory.getLogger(RatingRepository.class);


    private DataSource dataSource;

    public RatingRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void createOrUpdate(Rating rating) {
        String query = "SELECT giftId, userId, rating FROM ratings WHERE giftId = " + rating.getGiftId() + " AND userID = " + rating.getUserId();
        String query2 = "update ratings SET rating = ? WHERE giftId = ? AND userId = ?";
        String query3 = "insert into ratings(giftId, userId, rating) values (?, ?, ?)";

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)
        ) {
            if (rs.next()) {
                PreparedStatement preparedStatement = connection.prepareStatement(query2);
                preparedStatement.setInt(1, rating.getRating());
                preparedStatement.setInt(2, rating.getGiftId());
                preparedStatement.setInt(3, rating.getUserId());
                preparedStatement.executeUpdate();
                LOG.info("Rating for gift id: " + rating.getGiftId() + " successfully updated to rating score of: " + rating.getRating());
            } else {
                PreparedStatement preparedStatement = connection.prepareStatement(query3);
                preparedStatement.setInt(1, rating.getGiftId());
                preparedStatement.setInt(2, rating.getUserId());
                preparedStatement.setInt(3, rating.getRating());
                preparedStatement.executeUpdate();
                AuditLogger.
                        getAuditLogger(RatingRepository.class)
                        .auditChange(new Entity(
                                "rating.create",
                                String.valueOf(rating.getGiftId()) + " - " + String.valueOf(rating.getUserId()),
                                "---",
                                String.valueOf(rating.getRating())

                        ));
            }
        } catch (SQLException e) {
            LOG.warn("Rating update or create unsuccessful due to SQL exception: " + e);
        }
    }

    public List<Rating> getAll(String giftId) {
        List<Rating> ratingList = new ArrayList<>();
        String query = "SELECT giftId, userId, rating FROM ratings WHERE giftId = " + giftId;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            while (rs.next()) {
                ratingList.add(new Rating(rs.getInt(1), rs.getInt(2), rs.getInt(3)));
            }
            LOG.info("Get all ratings successful");
        } catch (SQLException e) {
            LOG.warn("Get all ratings unsuccessful due to SQL exception: " + e);
        }
        return ratingList;
    }
}
