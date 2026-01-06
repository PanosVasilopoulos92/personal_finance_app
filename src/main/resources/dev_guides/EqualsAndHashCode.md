VLAD MIHALCEA'S APPROACH TO EQUALS AND HASHCODE
================================================

Following the recommendations of Vlad Mihalcea, the foremost authority on 
Hibernate and JPA performance optimization.

REFERENCE: 
- https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
- https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/

WHY FOLLOW VLAD'S RECOMMENDATIONS?
==================================

Vlad Mihalcea has:
- 15+ years of Hibernate expertise
- Written the definitive book "High-Performance Java Persistence"
- Collaborated directly with the Hibernate team
- Analyzed thousands of real-world performance issues
- His recommendations are battle-tested in production systems

VLAD'S CORE PRINCIPLES
======================

1. IMPLEMENT equals/hashCode IN EACH ENTITY
   - More explicit and clear
   - Flexibility to use different strategies per entity
   - Easier to understand for new developers
   - No surprises from inheritance

2. USE instanceof INSTEAD OF getClass()
   - Works correctly with Hibernate proxies
   - More flexible with entity hierarchies
   - Recommended by Hibernate documentation
   - Prevents ClassCastException with proxies

3. USE BUSINESS KEY (NATURAL ID) FOR EQUALITY
   - Compare UUID, email, SKU, or other business identifiers
   - NOT the database-generated ID
   - More semantically correct
   - Works before and after persistence

4. USE STABLE hashCode
   - Return a constant value (like getClass().hashCode())
   - NEVER use mutable fields
   - NEVER use generated ID (it's null before persist)
   - Prevents entities from getting "lost" in HashSets

THE PATTERN
===========

```java
@Entity
public class YourEntity extends BaseEntity {
    
    @NaturalId  // Vlad recommends annotating business keys
    @Column(unique = true, nullable = false)
    private String businessKey;  // UUID, email, SKU, etc.
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof YourEntity)) return false;  // instanceof, not getClass()
        
        YourEntity that = (YourEntity) o;
        return businessKey != null && businessKey.equals(that.businessKey);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();  // Constant, stable value
    }
}
```

DEEP DIVE: WHY instanceof OVER getClass()?
==========================================

This is Vlad's key insight that many developers miss.

THE PROBLEM WITH getClass():
```java
// Your entity
Account account = new Account();

// Hibernate loads it lazily - creates a proxy
Account proxy = session.load(Account.class, 1L);

// Different classes!
account.getClass() != proxy.getClass()
// Account.class != Account$$HibernateProxy$$abc123.class

// They won't be equal even with same UUID!
```

THE SOLUTION WITH instanceof:
```java
Account account = new Account();
Account proxy = session.load(Account.class, 1L);

// Both pass instanceof check
proxy instanceof Account  // true!
account instanceof Account  // true!

// They can be equal if they have the same UUID ✓
```

HIBERNATE CREATES PROXIES FOR:
- Lazy-loaded entities
- Entities in @ManyToOne relationships with FetchType.LAZY
- Entities returned by session.load()

If you use getClass(), your equals() breaks with proxies!

DEEP DIVE: WHY BUSINESS KEY OVER DATABASE ID?
=============================================

Vlad strongly recommends using business keys (natural IDs) instead of database IDs.

PROBLEMS WITH DATABASE ID:
```java
// Database ID is null before persist
Account account1 = new Account();
Account account2 = new Account();

Set<Account> accounts = new HashSet<>();
accounts.add(account1);  // hashCode = something
accounts.add(account2);  // hashCode = something else

// After persist, IDs are assigned
entityManager.persist(account1);  // ID becomes 1
entityManager.persist(account2);  // ID becomes 2

// If you used ID in hashCode, it just changed!
// Now the entities are "lost" in the HashSet
accounts.contains(account1);  // FALSE! Even though it's there!
```

SOLUTION WITH BUSINESS KEY (UUID):
```java
Account account1 = new Account();  // UUID assigned in @PrePersist
Account account2 = new Account();  // UUID assigned in @PrePersist

Set<Account> accounts = new HashSet<>();
accounts.add(account1);  // hashCode = constant
accounts.add(account2);  // hashCode = constant

// After persist, IDs are assigned but hashCode hasn't changed
entityManager.persist(account1);  
entityManager.persist(account2);

// Still works correctly!
accounts.contains(account1);  // TRUE ✓
```

DEEP DIVE: WHY getClass().hashCode()?
=====================================

You might think: "All entities have the same hashCode? That's terrible for HashMap performance!"

Vlad's response: It's a necessary trade-off.

THE PROBLEM WITH uuid.hashCode():
```java
Account account = new Account();
int hash1 = account.hashCode();  // uuid is null, so hashCode = 0

Set<Account> set = new HashSet<>();
set.add(account);  // Stored in bucket determined by hash1

// @PrePersist fires
account.setUuid("generated-uuid");
int hash2 = account.hashCode();  // uuid.hashCode() - DIFFERENT VALUE!

// Now the account is in the WRONG BUCKET in the HashSet
set.contains(account);  // FALSE! Lost in the HashSet!
```

THE SOLUTION - CONSTANT HASHCODE:
```java
@Override
public int hashCode() {
    return getClass().hashCode();
}
```

Yes, all Account instances return the same hashCode.
Yes, this reduces HashMap efficiency (they all go to the same bucket).

BUT:
- HashMap still works (uses equals() to find the right object in the bucket)
- The performance impact is minimal in practice (how many entities in one HashSet?)
- It's CORRECT (never breaks the equals/hashCode contract)
- It's SAFE (entities never get lost in collections)

As Vlad says: "Correctness first, optimization second."

APPLYING THIS TO YOUR PROJECT
==============================

Your entities already have the perfect setup:
- UUID as business identifier ✓
- UUID set in @PrePersist ✓
- UUID is unique and stable ✓

You just need to:
1. Add @NaturalId to the uuid field in ThingT
2. Implement equals/hashCode in Account, Transaction, Category
3. Use the pattern shown above

EXAMPLE FROM YOUR PROJECT:

```java
@Entity
public class Account extends ThingT {
    
    // Your existing fields...
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account)) return false;
        Account account = (Account) o;
        return getUuid() != null && getUuid().equals(account.getUuid());
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
```

COMMON MISTAKES TO AVOID
=========================

❌ MISTAKE 1: Using database ID in equals
```java
@Override
public boolean equals(Object o) {
    Account that = (Account) o;
    return id != null && id.equals(that.id);  // WRONG!
}
```
Problem: Doesn't work before persistence, breaks in detached state

❌ MISTAKE 2: Using UUID in hashCode
```java
@Override
public int hashCode() {
    return uuid != null ? uuid.hashCode() : 0;  // WRONG!
}
```
Problem: HashCode changes when UUID is assigned, breaks collections

❌ MISTAKE 3: Using getClass() check
```java
@Override
public boolean equals(Object o) {
    if (getClass() != o.getClass()) return false;  // PROBLEMATIC!
}
```
Problem: Fails with Hibernate proxies, causes unexpected inequality

❌ MISTAKE 4: Including mutable fields in equals
```java
@Override
public boolean equals(Object o) {
    Account that = (Account) o;
    return accountName.equals(that.accountName);  // WRONG!
}
```
Problem: If accountName changes, entity gets lost in HashSet

✓ CORRECT PATTERN (Vlad's recommendation):
```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Account)) return false;
    Account that = (Account) o;
    return getUuid() != null && getUuid().equals(that.getUuid());
}

@Override
public int hashCode() {
    return getClass().hashCode();
}
```

TESTING YOUR IMPLEMENTATION
============================

Vlad emphasizes testing. Here's how to verify your implementation:

```java
@Test
void testEqualityWithHibernateProxy() {
    // Create real entity
    Account account = new Account();
    account.setUuid("test-uuid");
    
    // Simulate Hibernate proxy (in real code, you'd get this from session.load())
    Account proxy = createHibernateProxy(account);
    
    // Should be equal despite different class types
    assertEquals(account, proxy);
}

@Test
void testHashCodeStability() {
    Account account = new Account();
    int hashBefore = account.hashCode();
    
    // Simulate @PrePersist
    account.setUuid("generated-uuid");
    int hashAfter = account.hashCode();
    
    // HashCode must not change
    assertEquals(hashBefore, hashAfter);
}

@Test
void testUnpersistedEntitiesNotEqual() {
    Account account1 = new Account();
    Account account2 = new Account();
    
    // Different instances with no UUID should not be equal
    assertNotEquals(account1, account2);
}

@Test
void testPersistedEntitiesWithSameUuidAreEqual() {
    Account account1 = new Account();
    account1.setUuid("same-uuid");
    
    Account account2 = new Account();
    account2.setUuid("same-uuid");
    
    // Same UUID = equal
    assertEquals(account1, account2);
}
```

VLAD'S RECOMMENDATIONS SUMMARY
==============================

1. ✓ Implement equals/hashCode in EACH entity (not base class)
2. ✓ Use instanceof check (not getClass())
3. ✓ Compare business key/natural ID (UUID in your case)
4. ✓ Use constant hashCode (getClass().hashCode())
5. ✓ Annotate business key with @NaturalId
6. ✓ Test with Hibernate proxies
7. ✓ Never use mutable fields
8. ✓ Never use database-generated ID

FURTHER READING
===============

Highly recommended resources from Vlad Mihalcea:
- "High-Performance Java Persistence" (book)
- https://vladmihalcea.com/hibernate-facts-equals-and-hashcode/
- https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
- https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/

YouTube: Search for "Vlad Mihalcea equals hashCode" for detailed video explanations

CONCLUSION
==========

By following Vlad Mihalcea's approach, you're implementing equals/hashCode
in a way that:
- Works correctly with Hibernate's lazy loading and proxies
- Maintains proper semantics before and after persistence
- Prevents entities from getting lost in collections
- Is proven in thousands of production systems
- Is recommended by the Hibernate team themselves

This is professional-grade code that will serve you well in your career.
