package io.budgetapp.dao;

import io.budgetapp.application.NotFoundException;
import io.budgetapp.model.Transaction;
import io.budgetapp.model.User;
import io.budgetapp.model.form.report.SearchFilter;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 *
 */
public class TransactionDAO extends AbstractDAO<Transaction> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionDAO.class);

    public TransactionDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public Transaction addTransaction(Transaction transaction) {
        LOGGER.debug("Add transaction {}", transaction);
        return persist(transaction);
    }

    public List<Transaction> find(User user, Integer limit) {
        Query query = currentSession().createQuery("FROM Transaction t WHERE t.ledger.user = :user ORDER BY t.transactionOn DESC, t.id ASC");
        query.setParameter("user", user);
        query.setMaxResults(limit);
        return list(query);
    }

    public Transaction findById(long id) {
        Transaction transaction = get(id);
        if(transaction == null) {
            throw new NotFoundException();
        }

        return transaction;
    }

    public List<Transaction> findByLedger(User user, long ledgerId) {
        Criteria criteria = defaultCriteria();
        criteria.createAlias("t.ledger", "ledger");

        criteria.add(Restrictions.eq("ledger.id", ledgerId));
        criteria.add(Restrictions.eq("ledger.user", user));
        return list(criteria);
    }

    public List<Transaction> findByRecurring(User user, long recurringId) {
        Query query = currentSession().createQuery("FROM Transaction t WHERE t.ledger.user = :user AND t.recurring.id = :recurringId ORDER BY t.transactionOn DESC, t.id ASC");
        query.setParameter("user", user);
        query.setParameter("recurringId", recurringId);
        return list(query);
    }

    public List<Transaction> findByRange(User user, Date start, Date end) {



        Query query = currentSession().createQuery("FROM Transaction t WHERE t.ledger.user = :user AND t.transactionOn BETWEEN :start AND :end ORDER BY t.transactionOn DESC, t.id ASC");
        query
                .setParameter("user", user)
                .setParameter("start", start)
                .setParameter("end", end);

        return list(query);
    }

    public List<Transaction> findTransactions(User user, SearchFilter filter) {
        Criteria criteria = defaultCriteria();
        criteria.createAlias("t.ledger", "ledger");

        criteria.add(Restrictions.eq("ledger.user", user));

        if(filter.isAmountRange()) {
            criteria.add(Restrictions.between("amount", filter.getMinAmount(), filter.getMaxAmount()));
        } else if(filter.getMinAmount() != null) {
            criteria.add(Restrictions.ge("amount", filter.getMinAmount()));
        } else if(filter.getMaxAmount() != null) {
            criteria.add(Restrictions.le("amount", filter.getMaxAmount()));
        }

        if(filter.isDateRange()) {
            criteria.add(Restrictions.between("transactionOn", filter.getStartOn(), filter.getEndOn()));
        } else if(filter.getStartOn() != null) {
            criteria.add(Restrictions.ge("transactionOn", filter.getStartOn()));
        } else if(filter.getEndOn() != null) {
            criteria.add(Restrictions.le("transactionOn", filter.getEndOn()));
        }

        return list(criteria);
    }


    private Criteria defaultCriteria() {
        Criteria criteria = currentSession().createCriteria(Transaction.class, "t");
        criteria.addOrder(Order.desc("transactionOn"));
        criteria.addOrder(Order.desc("id"));
        return criteria;
    }
}