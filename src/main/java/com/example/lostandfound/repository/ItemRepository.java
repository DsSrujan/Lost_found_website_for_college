package com.example.lostandfound.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.lostandfound.model.Item;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    
    List<Item> findByStatusOrderByCreatedAtDesc(Item.ItemStatus status);
    
    List<Item> findByCategoryOrderByCreatedAtDesc(String category);
    
    List<Item> findByUserOrderByCreatedAtDesc(com.example.lostandfound.model.User user);
    
    void deleteByIdAndUser(Long id, com.example.lostandfound.model.User user);
    
    Page<Item> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    @Query("SELECT i FROM Item i WHERE " +
           "LOWER(i.title) LIKE LOWER(:keyword) OR " +
           "LOWER(i.description) LIKE LOWER(:keyword) OR " +
           "LOWER(i.category) LIKE LOWER(:keyword) " +
           "ORDER BY i.createdAt DESC")
    List<Item> searchItems(@Param("keyword") String keyword);
    
    @Query("SELECT i FROM Item i WHERE " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:category IS NULL OR LOWER(i.category) = LOWER(:category)) AND " +
           "(LOWER(i.title) LIKE LOWER(:keyword) OR " +
           "LOWER(i.description) LIKE LOWER(:keyword) OR " +
           "LOWER(i.category) LIKE LOWER(:keyword)) " +
           "ORDER BY i.createdAt DESC")
    List<Item> searchItemsWithFilters(@Param("keyword") String keyword, 
                                     @Param("status") Item.ItemStatus status, 
                                     @Param("category") String category);
}
