package com.example.doan10diem.repository;

import com.example.doan10diem.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByParentIsNullOrderByNameAsc();

    List<Category> findByParentOrderByNameAsc(Category parent);

    List<Category> findByParent_IdOrderByNameAsc(Long parentId);

    List<Category> findAllByOrderByNameAsc();

    List<Category> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

    /** Lấy tất cả Root với children đã fetch (tránh LazyInitializationException). */
    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.children WHERE c.parent IS NULL ORDER BY c.name")
    List<Category> findAllRootsWithChildrenOrderByName();

    /** Lấy Root theo danh sách id, kèm children đã fetch (dùng cho kết quả tìm kiếm). */
    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.children WHERE c.parent IS NULL AND c.id IN :ids ORDER BY c.name")
    List<Category> findRootsByIdInWithChildrenOrderByName(@Param("ids") Set<Long> ids);
}
