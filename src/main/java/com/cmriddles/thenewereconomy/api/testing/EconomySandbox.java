package com.cmriddles.thenewereconomy.api.testing;
import com.cmriddles.thenewereconomy.api.EconomyClient;
import com.cmriddles.thenewereconomy.api.TneApi;
import com.cmriddles.thenewereconomy.api.account.Account;
import com.cmriddles.thenewereconomy.api.account.AccountRef;
import com.cmriddles.thenewereconomy.api.account.AccountService;
import com.cmriddles.thenewereconomy.api.account.AccountState;
import com.cmriddles.thenewereconomy.api.account.AccountType;
import com.cmriddles.thenewereconomy.api.currency.CurrencyId;
import com.cmriddles.thenewereconomy.api.currency.CurrencyInfo;
import com.cmriddles.thenewereconomy.api.currency.ScopeId;
import com.cmriddles.thenewereconomy.api.economy.AccountBalance;
import com.cmriddles.thenewereconomy.api.economy.Balance;
import com.cmriddles.thenewereconomy.api.economy.Payment;
import com.cmriddles.thenewereconomy.api.economy.Transaction;
import com.cmriddles.thenewereconomy.api.economy.TransactionEntry;
import com.cmriddles.thenewereconomy.api.economy.TransactionReceipt;
import com.cmriddles.thenewereconomy.api.economy.TransactionRequest;
import com.cmriddles.thenewereconomy.api.extension.ApiCapabilities;
import com.cmriddles.thenewereconomy.api.result.CorrelationId;
import com.cmriddles.thenewereconomy.api.result.Failure;
import com.cmriddles.thenewereconomy.api.result.Result;
import com.cmriddles.thenewereconomy.api.result.ResultCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
/**
 * Deterministic in-memory fixture for plugin unit tests, requiring no Minecraft server.
 * Supports core accounts, balances, atomic payments, receipts, history and full reversals.
 * Use a real database-backed test server for reservations, workflows and provider conformance.
 * Seed methods affect only this isolated fixture; they never access a running TNE instance.
 */
public final class EconomySandbox implements TneApi {
 private final State state;
 private volatile boolean available=true;
 private ResultCode nextFailure;
 private boolean loseReply;
 public EconomySandbox(){this(new State());}
 private EconomySandbox(State state){this.state=state;}
 /** A new provider retaining this fixture's financial state, simulating a process restart. */
 public EconomySandbox restart(){return new EconomySandbox(state);}
 public void available(boolean value){available=value;}
 public synchronized void failNextPayment(ResultCode code){nextFailure=Objects.requireNonNull(code);}
 public synchronized void loseNextPaymentReply(){loseReply=true;}
 public void seedPlayer(UUID id,String name,String currency,BigDecimal balance){
  Objects.requireNonNull(balance);if(balance.signum()<0)throw new IllegalArgumentException("Balance cannot be negative");
  synchronized(state){state.accounts.put(id,new Account(id,name,AccountType.PLAYER,AccountState.ACTIVE,Optional.empty(),Optional.empty()));state.balances.put(new Key(id,new CurrencyId(currency).value()),balance.setScale(2,java.math.RoundingMode.UNNECESSARY));}
 }
 @Override public boolean isAvailable(){return available;}
 @Override public CurrencyInfo defaultCurrency(){return new CurrencyInfo("coins","Coin","Coins","$",2);}
 @Override public ScopeId defaultScope(){return new ScopeId("network");}
 @Override public List<CurrencyInfo> currencies(){return List.of(defaultCurrency());}
 @Override public ApiCapabilities capabilities(){return new ApiCapabilities("sandbox",Set.of("accounts","payments","receipts","history"));}
 @Override public EconomyClient client(String name){if(name==null||name.isBlank())throw new IllegalArgumentException("Integration name required");return new Client(name);}
 private final class Client implements EconomyClient {
  private final String name;
  Client(String name){this.name=name;}
  @Override public String integrationName(){return name;}
  @Override public ScopeId scope(){return defaultScope();}
  @Override public CurrencyInfo defaultCurrency(){return EconomySandbox.this.defaultCurrency();}
  @Override public AccountService accounts(){return new AccountService(){
   @Override public CompletionStage<Result<Account>> create(String key,String label){return call(()->{String k=key.toLowerCase(Locale.ROOT);if(!k.matches("[a-z0-9][a-z0-9_.:-]{0,63}"))throw new IllegalArgumentException("Invalid account key");String lookup=name+"/"+k;UUID id=state.keys.computeIfAbsent(lookup,x->UUID.nameUUIDFromBytes(x.getBytes(java.nio.charset.StandardCharsets.UTF_8)));return state.accounts.computeIfAbsent(id,x->new Account(x,label,AccountType.PLUGIN,AccountState.ACTIVE,Optional.of(name),Optional.of(k)));});}
   @Override public CompletionStage<Result<Optional<Account>>> find(String key){return call(()->Optional.ofNullable(state.keys.get(name+"/"+key.toLowerCase(Locale.ROOT))).map(state.accounts::get));}
   @Override public CompletionStage<Result<Optional<Account>>> find(AccountRef ref){return call(()->Optional.ofNullable(state.accounts.get(ref.id())));}
   @Override public CompletionStage<Result<List<Account>>> list(int limit,int offset){return call(()->{page(limit,offset);return state.accounts.values().stream().filter(a->a.integrationName().filter(name::equals).isPresent()).sorted(Comparator.comparing(Account::name).thenComparing(a->a.id().toString())).skip(offset).limit(limit).toList();});}
   @Override public CompletionStage<Result<Account>> rename(String key,String label){return call(()->{UUID id=state.keys.get(name+"/"+key.toLowerCase(Locale.ROOT));Account a=Objects.requireNonNull(state.accounts.get(id),"Account not found");Account renamed=new Account(id,label,a.type(),a.state(),a.integrationName(),a.key());state.accounts.put(id,renamed);return renamed;});}
  };}
  @Override public CompletionStage<Result<AccountBalance>> balance(AccountRef ref,String currency){return call(()->{currency(currency);Account account=Objects.requireNonNull(state.accounts.get(ref.id()),"Account not found");return new AccountBalance(account,"coins",scope(),state.balances.getOrDefault(new Key(ref.id(),"coins"),BigDecimal.ZERO.setScale(2)),state.sequence);});}
  @Override public CompletionStage<Result<Balance>> balance(UUID player,String currency){return balance(AccountRef.player(player),currency).thenApply(r->r.isSuccess()?Result.success(new Balance(player,currency,scope(),r.value().amount(),r.value().version())):Result.failure(r.failure()));}
  @Override public CompletionStage<Result<List<AccountBalance>>> balances(AccountRef ref){return balance(ref,"coins").thenApply(r->r.isSuccess()?Result.success(List.of(r.value())):Result.failure(r.failure()));}
  @Override public CompletionStage<Result<TransactionReceipt>> post(TransactionRequest request){return postInternal(request,Map.of());}
  private CompletionStage<Result<TransactionReceipt>> postInternal(TransactionRequest request,Map<String,String> extra){
   synchronized(state){
    if(!available)return done(fail(ResultCode.DEPENDENCY_UNAVAILABLE,"Fixture unavailable"));
    synchronized(EconomySandbox.this){if(nextFailure!=null){ResultCode code=nextFailure;nextFailure=null;return done(fail(code,"Injected failure before commit"));}}
    try{
     String key=name+"/"+request.idempotencyKey();if(state.receipts.containsKey(key))return done(fail(ResultCode.CONFLICT,"Operation already committed"));
     Map<Key,BigDecimal> deltas=new HashMap<>();List<TransactionEntry> entries=new ArrayList<>();
     for(Payment p:request.payments()){
      currency(p.currency());if(p.amount().signum()<=0)throw new IllegalArgumentException("Positive amount required");BigDecimal amount=p.amount().setScale(2,java.math.RoundingMode.UNNECESSARY);
      if(p.source().id().equals(p.destination().id()))throw new IllegalArgumentException("Accounts must differ");
      for(boolean debit:List.of(true,false)){UUID id=(debit?p.source():p.destination()).id();Account a=Objects.requireNonNull(state.accounts.get(id),"Account not found");BigDecimal signed=debit?amount.negate():amount;deltas.merge(new Key(id,"coins"),signed,BigDecimal::add);entries.add(new TransactionEntry(a.ref(),a.name(),"coins",scope(),signed));}
     }
     for(var delta:deltas.entrySet())if(state.balances.getOrDefault(delta.getKey(),BigDecimal.ZERO).add(delta.getValue()).signum()<0)return done(fail(ResultCode.INSUFFICIENT_FUNDS,"Insufficient funds"));
     UUID id=new UUID(0,++state.sequence);Map<String,String> metadata=new HashMap<>(request.metadata());metadata.putAll(extra);
     var receipt=new TransactionReceipt(id,id,name,request.idempotencyKey(),Instant.EPOCH.plusSeconds(state.sequence),entries,metadata);
     for(var delta:deltas.entrySet())state.balances.merge(delta.getKey(),delta.getValue(),BigDecimal::add);
     state.receipts.put(key,receipt);state.byId.put(id,receipt);
     synchronized(EconomySandbox.this){if(loseReply){loseReply=false;return done(fail(ResultCode.INTERNAL_ERROR,"Injected lost response after commit; reconcile the saved key"));}}
     return done(Result.success(receipt));
    }catch(IllegalArgumentException|ArithmeticException|NullPointerException invalid){return done(fail(ResultCode.VALIDATION_ERROR,"Invalid sandbox transaction"));}
   }
  }
  @Override public CompletionStage<Result<Transaction>> transfer(AccountRef source,AccountRef destination,String currency,BigDecimal amount,String key){return post(new TransactionRequest(List.of(new Payment(source,destination,currency,amount)),key)).thenApply(r->r.isSuccess()?Result.success(new Transaction(r.value().transactionId(),currency,scope(),amount)):Result.failure(r.failure()));}
  @Override public CompletionStage<Result<Transaction>> transfer(UUID source,UUID destination,String currency,BigDecimal amount,String key){return transfer(AccountRef.player(source),AccountRef.player(destination),currency,amount,key);}
  @Override public CompletionStage<Result<Transaction>> deposit(UUID player,String currency,BigDecimal amount,String key){return transfer(AccountRef.account(new UUID(0,1)),AccountRef.player(player),currency,amount,key);}
  @Override public CompletionStage<Result<Transaction>> withdraw(UUID player,String currency,BigDecimal amount,String key){return transfer(AccountRef.player(player),AccountRef.account(new UUID(0,1)),currency,amount,key);}
  @Override public CompletionStage<Result<Optional<TransactionReceipt>>> receipt(String key){return call(()->Optional.ofNullable(state.receipts.get(name+"/"+key)));}
  @Override public CompletionStage<Result<Optional<TransactionReceipt>>> receipt(UUID id){return call(()->Optional.ofNullable(state.byId.get(id)).filter(r->r.integrationName().equals(name)));}
  @Override public CompletionStage<Result<Optional<Transaction>>> transaction(String key){return receipt(key).thenApply(r->r.isSuccess()?Result.success(r.value().map(x->{if(x.entries().size()!=2)throw new IllegalArgumentException("Use receipt for batch transactions");var first=x.entries().getFirst();return new Transaction(x.transactionId(),first.currency(),scope(),first.amount().abs());})):Result.failure(r.failure()));}
  @Override public CompletionStage<Result<List<TransactionReceipt>>> history(AccountRef account,int limit,int offset){return call(()->{page(limit,offset);return state.byId.values().stream().filter(r->r.entries().stream().anyMatch(e->e.account().id().equals(account.id()))).sorted(Comparator.comparing(TransactionReceipt::createdAt).reversed()).skip(offset).limit(limit).toList();});}
  @Override public CompletionStage<Result<TransactionReceipt>> reverse(UUID original,String reason,String key){synchronized(state){
   var receipt=state.byId.get(original);if(receipt==null||!receipt.integrationName().equals(name))return done(fail(ResultCode.PERMISSION_DENIED,"Original transaction unavailable"));
   if(state.reversed.contains(original)||receipt.metadata().containsKey("reversesTransactionId"))return done(fail(ResultCode.CONFLICT,"Already reversed or itself a reversal"));
   List<Payment> payments=new ArrayList<>();for(int i=0;i<receipt.entries().size();i+=2){var debit=receipt.entries().get(i);var credit=receipt.entries().get(i+1);payments.add(new Payment(credit.account(),debit.account(),credit.currency(),credit.amount()));}
   var result=postInternal(new TransactionRequest(payments,key),Map.of("reversesTransactionId",original.toString(),"publicReason",reason)).toCompletableFuture().join();
   if(result.isSuccess()||state.receipts.containsKey(name+"/"+key))state.reversed.add(original);return done(result);
  }}
  private <T>CompletionStage<Result<T>> call(java.util.function.Supplier<T> action){synchronized(state){if(!available)return done(fail(ResultCode.DEPENDENCY_UNAVAILABLE,"Fixture unavailable"));try{return done(Result.success(action.get()));}catch(IllegalArgumentException|NullPointerException invalid){return done(fail(ResultCode.VALIDATION_ERROR,"Invalid sandbox request"));}}}
 }
 private static void currency(String currency){if(!new CurrencyId(currency).value().equals("coins"))throw new IllegalArgumentException("Sandbox supports coins");}
 private static void page(int limit,int offset){if(limit<1||limit>100||offset<0)throw new IllegalArgumentException("Invalid page");}
 private static <T>Result<T> fail(ResultCode code,String message){return Result.failure(new Failure(code,message,new CorrelationId(new UUID(0,0))));}
 private static <T>CompletionStage<Result<T>> done(Result<T> result){return CompletableFuture.completedFuture(result);}
 private record Key(UUID account,String currency) { }
 private static final class State {
  private final Map<UUID,Account> accounts=new LinkedHashMap<>();private final Map<String,UUID> keys=new HashMap<>();private final Map<Key,BigDecimal> balances=new HashMap<>();
  private final Map<String,TransactionReceipt> receipts=new HashMap<>();private final Map<UUID,TransactionReceipt> byId=new LinkedHashMap<>();private final Set<UUID> reversed=new HashSet<>();private long sequence;
 }
}
